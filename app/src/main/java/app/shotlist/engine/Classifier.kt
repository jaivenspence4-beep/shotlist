package app.shotlist.engine

import app.shotlist.data.Finding

/**
 * Rule-scoring Tier-1 classifier (per docs/research extraction tiers): turns OCR
 * text + extracted signals into zero or more Findings. Deliberately conservative —
 * a meme in the inbox costs more trust than a missed flyer.
 */
object Classifier {

    private val eventWords = listOf(
        "tickets", "doors", "rsvp", "show", "concert", "live", "event", "party",
        "festival", "tour", "admission", "venue", "presents", "performance",
        "invited", "join us", "save the date",
    )
    private val deadlineWords = listOf(
        "due", "deadline", "expires", "expiration", "last day", "ends", "closing",
        "final day", "renew", "payment due", "register by", "apply by",
    )
    private val productWords = listOf(
        "add to cart", "buy", "checkout", "in stock", "free shipping", "sale",
        "% off", "order", "wishlist", "price",
    )
    private val recipeWords = listOf(
        "ingredients", "cup", "cups", "tbsp", "tsp", "oven", "preheat", "bake",
        "simmer", "recipe", "servings",
    )

    /** Suggestions below this never reach the inbox — trust beats recall. */
    private const val CONFIDENCE_FLOOR = 0.55f

    /** More than this per screenshot is noise by definition. */
    private const val MAX_FINDINGS_PER_SHOT = 4

    // Residual clock/status text ("10:34 Mon, Aug 31 ...") that survives the
    // bounding-box strip, e.g. inside screenshots OF screenshots.
    private val clockLine = Regex(
        "^\\s*\\d{1,2}:\\d{2}\\s*(?:am|pm)?\\s*[·,]?\\s*" +
            "(mon|tue|wed|thu|fri|sat|sun)\\b.*$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )

    fun classify(shotId: Long, text: String, s: Extractor.Signals): List<Finding> {
        val cleaned = clockLine.replace(text, "")
        val lower = cleaned.lowercase()
        val out = mutableListOf<Finding>()
        val title = titleFrom(cleaned)

        val eventScore = score(lower, eventWords)
        val deadlineScore = score(lower, deadlineWords)

        // Field data (116 findings from 100 real screenshots, 87 bogus events)
        // killed the old anchorless branch: a date/time alone is NEVER an
        // event. It must co-occur with semantic anchors.
        val datetime = if (clockLine.containsMatchIn(s.datetime?.matched.orEmpty())) null
        else s.datetime
        if (datetime != null && (eventScore > 0 || deadlineScore > 0)) {
            val isDeadline = deadlineScore > eventScore
            out += Finding(
                shotId = shotId,
                type = if (isDeadline) "DEADLINE" else "EVENT",
                title = title,
                snippet = datetime.matched,
                whenAt = DateTimeParser.toEpochMillis(datetime),
                payload = s.addressLine.orEmpty(),
                confidence = (datetime.confidence + 0.1f * (eventScore + deadlineScore))
                    .coerceAtMost(1f),
            )
        }

        if (s.prices.isNotEmpty() && score(lower, productWords) > 0) {
            out += Finding(
                shotId = shotId,
                type = "PRODUCT",
                title = title,
                amountCents = s.prices.max(),
                confidence = 0.6f,
            )
        }

        s.wifi?.let { (ssid, pw) ->
            out += Finding(
                shotId = shotId, type = "WIFI", title = ssid,
                payload = pw, confidence = 0.85f,
            )
        }
        s.codes.forEach { code ->
            out += Finding(
                shotId = shotId, type = "CODE", title = "Code $code",
                payload = code, confidence = 0.7f,
            )
        }
        s.tracking.take(1).forEach { t ->
            out += Finding(
                shotId = shotId, type = "TRACKING", title = "Package $t",
                payload = t, confidence = 0.6f,
            )
        }
        if (score(lower, recipeWords) >= 2) {
            out += Finding(shotId = shotId, type = "RECIPE", title = title, confidence = 0.6f)
        }

        // Precision gate: floor, then best-per-type (one screenshot is one
        // event, not five), then a hard cap.
        return out
            .filter { it.confidence >= CONFIDENCE_FLOOR }
            .groupBy { it.type }
            .map { (_, group) -> group.maxBy { it.confidence } }
            .sortedByDescending { it.confidence }
            .take(MAX_FINDINGS_PER_SHOT)
    }

    private fun score(lower: String, words: List<String>): Int =
        words.count { lower.contains(it) }

    /** First non-trivial OCR line becomes the card title. */
    private fun titleFrom(text: String): String =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length in 4..64 && it.any(Char::isLetter) }
            ?.take(64)
            ?: "Screenshot"
}
