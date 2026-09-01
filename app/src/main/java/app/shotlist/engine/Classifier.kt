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

    fun classify(shotId: Long, text: String, s: Extractor.Signals): List<Finding> {
        val lower = text.lowercase()
        val out = mutableListOf<Finding>()
        val title = titleFrom(text)

        val eventScore = score(lower, eventWords)
        val deadlineScore = score(lower, deadlineWords)

        if (s.datetime != null && (eventScore > 0 || deadlineScore > 0)) {
            val isDeadline = deadlineScore > eventScore
            out += Finding(
                shotId = shotId,
                type = if (isDeadline) "DEADLINE" else "EVENT",
                title = title,
                snippet = s.datetime.matched,
                whenAt = DateTimeParser.toEpochMillis(s.datetime),
                payload = s.addressLine.orEmpty(),
                confidence = (s.datetime.confidence + 0.1f * (eventScore + deadlineScore))
                    .coerceAtMost(1f),
            )
        } else if (s.datetime != null && s.datetime.confidence >= 0.6f) {
            // Explicit date but no keyword — still worth a low-confidence suggestion.
            out += Finding(
                shotId = shotId,
                type = "EVENT",
                title = title,
                snippet = s.datetime.matched,
                whenAt = DateTimeParser.toEpochMillis(s.datetime),
                payload = s.addressLine.orEmpty(),
                confidence = 0.45f,
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
                payload = t, confidence = 0.5f,
            )
        }
        if (score(lower, recipeWords) >= 2) {
            out += Finding(shotId = shotId, type = "RECIPE", title = title, confidence = 0.6f)
        }

        return out
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
