package app.shotlist.engine

import app.shotlist.data.Finding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Rule-scoring Tier-1 classifier. Shaped by two rounds of real S26 field data
 * (116 findings/100 shots, then 43/100 — both too noisy): inbox trust beats
 * recall, so every gate here exists because a real false positive demanded it.
 */
object Classifier {

    /**
     * BUMP THIS ON EVERY CHANGE TO PERSISTED OUTPUT — titles, gates, snippets,
     * anything. Stored SUGGESTED findings from older versions are purged and
     * shots re-scanned (EngineApi). Field-proven failure mode: round-3 gates
     * shipped without a bump and stale junk survived on-device through two
     * installs. If you touched this file, you almost certainly bump this.
     */
    const val VERSION = 5

    /** Suggestions below this never reach the inbox. */
    private const val CONFIDENCE_FLOOR = 0.55f

    /** More than this per screenshot is noise by definition. */
    private const val MAX_FINDINGS_PER_SHOT = 3

    // "live" and "show" removed: every TikTok/IG profile screenshot contains
    // "Live"/"Following", which minted 1.0-confidence garbage events.
    private val eventWords = listOf(
        "tickets", "doors", "rsvp", "concert", "festival", "tour", "admission",
        "venue", "presents", "performance", "invited", "join us", "save the date",
        "open house", "showing", "appointment", "reservation", "check-in",
    )
    private val deadlineWords = listOf(
        "due", "deadline", "expires", "expiration", "last day", "ends", "closing",
        "final day", "renew", "payment due", "register by", "apply by",
    )
    private val productWords = listOf(
        "add to cart", "buy now", "checkout", "in stock", "free shipping",
        "% off", "sale price", "list price", "order", "wishlist",
    )
    private val recipeWords = listOf(
        "ingredients", "cup", "cups", "tbsp", "tsp", "oven", "preheat", "bake",
        "simmer", "recipe", "servings",
    )

    private val clockLine = Regex(
        "^\\s*\\d{1,2}:\\d{2}\\s*(?:am|pm)?\\s*[·,]?\\s*" +
            "(mon|tue|wed|thu|fri|sat|sun)\\b.*$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )

    // Lines that must never become card titles: URLs, emails, UI fragments.
    // Domain tokens are rejected even without a trailing slash — OCR mangles
    // "order.littlecaesars.com" into fragments that dodged the old pattern.
    // No trailing \b on domain suffixes: OCR concatenates ("tiktok.comview")
    // and the boundary let it through. Over-rejection is safe for titles —
    // the next line gets a chance instead.
    private val junkTitle = Regex(
        "(://|www\\.|@\\w+\\.|^\\W+$|^[%/|•·\\-_=+ ]|\\.\\w{2,4}/|" +
            "\\.(com|net|org|io|co|app|gov|edu|me|tv))",
        RegexOption.IGNORE_CASE,
    )
    private val uiNoiseTitles = setOf(
        "done", "follow", "following", "followers", "subscribe", "like", "likes",
        "share", "views", "comments", "next", "back", "ok", "cancel", "menu",
        "home", "search", "live", "reply",
    )

    /** "7 Follow", "25 likes" — social counters are not headlines. */
    private fun isSocialCounter(line: String): Boolean =
        line.lowercase().replace(Regex("[^a-z ]"), "").trim() in uiNoiseTitles

    private val whenFormat = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")

    fun classify(shotId: Long, text: String, s: Extractor.Signals): List<Finding> {
        val cleaned = clockLine.replace(text, "")
        val lower = cleaned.lowercase()
        val out = mutableListOf<Finding>()
        val title = titleFrom(cleaned)

        val eventScore = score(lower, eventWords)
        val deadlineScore = score(lower, deadlineWords)

        val datetime = s.datetime?.takeUnless {
            clockLine.containsMatchIn(it.matched)
        }
        // Field law, round 2: an EVENT needs a real DATE (time-only "today
        // 00:48" is a chat timestamp — every messaging screenshot has them),
        // a non-junk title, and anchors. Relative dates ("today"/"tomorrow")
        // need TWO anchors because chat UIs print those words constantly.
        val hasRealDate = datetime != null && datetime.confidence >= 0.6f
        val isRelative = datetime?.matched?.lowercase() in
            setOf("today", "tonight", "tomorrow")
        val anchors = maxOf(eventScore, deadlineScore)
        val anchorsNeeded = if (isRelative == true) 2 else 1

        // A past event is not actionable: explicit-year dates ("Aug 23, 2026"
        // in an old letter) dodge the parser's future-bias and minted cards
        // for things already over.
        val nowMs = System.currentTimeMillis()
        if (hasRealDate && anchors >= anchorsNeeded && title != null &&
            DateTimeParser.toEpochMillis(datetime!!) > nowMs - 24 * 3600_000L
        ) {
            val isDeadline = deadlineScore > eventScore
            val whenAt = DateTimeParser.toEpochMillis(datetime)
            out += Finding(
                shotId = shotId,
                type = if (isDeadline) "DEADLINE" else "EVENT",
                title = title,
                snippet = describeWhen(whenAt, isDeadline, s.addressLine),
                whenAt = whenAt,
                payload = s.addressLine.orEmpty(),
                confidence = (datetime.confidence + 0.1f * anchors).coerceAtMost(1f),
            )
        }

        // PRODUCT: two anchors, sane price, real title — and link-heavy pages
        // (search results, social feeds) need a third anchor, since they are
        // where the surviving false products came from.
        val bestPrice = s.prices.filter { it in 99..10_000_00 }.maxOrNull()
        val productAnchors = score(lower, productWords)
        val productBar = if (s.urls.size > 1) 3 else 2
        if (bestPrice != null && productAnchors >= productBar && title != null) {
            out += Finding(
                shotId = shotId,
                type = "PRODUCT",
                title = title,
                snippet = "Spotted at ${formatPrice(bestPrice)} — keeping it for you",
                amountCents = bestPrice,
                confidence = 0.65f,
            )
        }

        // WIFI: needs a real SSID hit and a plausible password — login screens
        // OCR "Password ••••••" and minted ten identical junk cards.
        s.wifi?.let { (ssid, pw) ->
            if (ssid != "Wi-Fi" && plausiblePassword(pw)) {
                out += Finding(
                    shotId = shotId, type = "WIFI", title = ssid,
                    snippet = "Wi-Fi password saved — tap to copy when you need it",
                    payload = pw, confidence = 0.85f,
                    // Sensitive by nature: vaults itself, zero user effort.
                    vaulted = true,
                )
            }
        }
        s.codes.firstOrNull()?.let { code ->
            out += Finding(
                shotId = shotId, type = "CODE", title = "Code for ${title ?: "later"}",
                snippet = "Tap to copy — hidden until you need it",
                payload = code, confidence = 0.7f,
                vaulted = true,
            )
        }
        s.tracking.take(1).forEach { t ->
            out += Finding(
                shotId = shotId, type = "TRACKING", title = "A package is moving",
                snippet = "Tracking number saved from your screenshot",
                payload = t, confidence = 0.6f,
            )
        }
        if (score(lower, recipeWords) >= 2 && title != null) {
            out += Finding(
                shotId = shotId, type = "RECIPE", title = title,
                snippet = "Recipe kept — ingredients ready when you are",
                confidence = 0.6f,
            )
        }

        return out
            .filter { it.confidence >= CONFIDENCE_FLOOR }
            .groupBy { it.type }
            .map { (_, group) -> group.maxBy { it.confidence } }
            .sortedByDescending { it.confidence }
            .take(MAX_FINDINGS_PER_SHOT)
    }

    private fun score(lower: String, words: List<String>): Int =
        words.count { lower.contains(it) }

    /** First line that could be a headline a human would accept. */
    private fun titleFrom(text: String): String? =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 4..64 &&
                    line.any(Char::isLetter) &&
                    !junkTitle.containsMatchIn(line) &&
                    !isSocialCounter(line) &&
                    line.lowercase().trim('!', '.', ' ') !in uiNoiseTitles &&
                    line.count(Char::isDigit) < line.length / 2
            }
            ?.take(64)

    private fun describeWhen(whenAt: Long, deadline: Boolean, address: String?): String {
        val stamp = Instant.ofEpochMilli(whenAt).atZone(ZoneId.systemDefault())
            .format(whenFormat)
        val head = if (deadline) "Due $stamp" else stamp
        return if (address != null) "$head · $address" else head
    }

    private fun formatPrice(cents: Long): String =
        if (cents % 100 == 0L) "$${cents / 100}" else "$${cents / 100}.%02d".format(cents % 100)

    private fun plausiblePassword(pw: String): Boolean =
        pw.length in 6..32 &&
            pw.none { it == '•' || it == '*' || it == '.' } &&
            pw.toSet().size > 2
}
