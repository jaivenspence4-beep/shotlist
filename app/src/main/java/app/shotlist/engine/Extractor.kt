package app.shotlist.engine

/**
 * Pure-Kotlin signal extraction over OCR text. Each hit is a candidate the
 * Classifier turns into (or folds into) a Finding.
 */
object Extractor {

    data class Signals(
        val datetime: DateTimeParser.Parsed?,
        val prices: List<Long>,          // cents
        val urls: List<String>,
        val phones: List<String>,
        val wifi: Pair<String, String>?, // ssid to password
        val codes: List<String>,         // door/gate/2FA-looking codes
        val tracking: List<String>,
        val addressLine: String?,
    )

    private val priceRe = Regex("[$€£]\\s?(\\d{1,5})(?:[.,](\\d{2}))?")
    private val urlRe = Regex(
        "\\b(?:https?://|www\\.)[\\w.-]+\\.[a-z]{2,}(?:/\\S*)?",
        RegexOption.IGNORE_CASE,
    )
    private val phoneRe =
        Regex("\\b(?:\\+?1[\\s.-]?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\\b")

    private val wifiSsidRe = Regex(
        "(?:wi-?fi|network|ssid)\\s*[:=]?\\s*([\\w' -]{2,32})",
        RegexOption.IGNORE_CASE,
    )
    private val passwordRe = Regex(
        "(?:password|pass|pw|pwd)\\s*[:=]?\\s*(\\S{4,32})",
        RegexOption.IGNORE_CASE,
    )
    private val codeRe = Regex(
        "(?:code|pin|gate|door|access|entry)\\s*[:#]?\\s*(\\d{3,8})",
        RegexOption.IGNORE_CASE,
    )

    // Branded formats stand alone; the loose FedEx numerics matched a Teams
    // meeting id in the field, so they only count next to shipping language.
    private val brandedTrackingRes = listOf(
        Regex("\\b1Z[0-9A-Z]{16}\\b"),                  // UPS
        Regex("\\b(?:94|93|92|95)\\d{18,20}\\b"),        // USPS
    )
    private val looseTrackingRe = Regex("\\b\\d{12}\\b|\\b\\d{15}\\b")
    private val shippingAnchor = Regex(
        "(tracking|package|shipped|shipment|fedex|ups|usps|out for delivery)",
        RegexOption.IGNORE_CASE,
    )

    private val addressRe = Regex(
        "\\b\\d{1,5}\\s+[A-Za-z0-9. ]{2,40}\\s" +
            "(?:st(?:reet)?|ave(?:nue)?|blvd|boulevard|rd|road|dr(?:ive)?|ln|lane|" +
            "way|ct|court|pl|place)\\b\\.?",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String): Signals {
        val prices = priceRe.findAll(text).mapNotNull { m ->
            val whole = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val cents = m.groupValues[2].toLongOrNull() ?: 0
            whole * 100 + cents
        }.toList()

        val wifiSsid = wifiSsidRe.find(text)?.groupValues?.get(1)?.trim()
        val password = passwordRe.find(text)?.groupValues?.get(1)
        val wifi = if (password != null) (wifiSsid ?: "Wi-Fi") to password else null

        // A code match that is actually the wifi password should not double-count.
        val codes = codeRe.findAll(text)
            .map { it.groupValues[1] }
            .filter { it != password }
            .distinct()
            .toList()

        val tracking = buildList {
            brandedTrackingRes.forEach { re ->
                re.findAll(text).forEach { add(it.value) }
            }
            if (shippingAnchor.containsMatchIn(text)) {
                looseTrackingRe.findAll(text).forEach { add(it.value) }
            }
        }.distinct()

        return Signals(
            datetime = DateTimeParser.parse(text),
            prices = prices,
            urls = urlRe.findAll(text).map { it.value }.distinct().toList(),
            phones = phoneRe.findAll(text).map { it.value.trim() }.distinct().toList(),
            wifi = wifi,
            codes = codes,
            tracking = tracking,
            addressLine = addressRe.find(text)?.value?.trim(),
        )
    }
}
