package app.shotlist.engine

/** Shared gate for any OCR-derived text promoted to a user-facing headline. */
internal object TitleQuality {
    // Domain tokens are rejected even without a trailing slash. No trailing
    // word boundary on suffixes: OCR can concatenate "tiktok.comview".
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
    private val lettersAndSpacesOnly = Regex("[^a-z ]")

    /** First line that could be a headline a human would accept. */
    fun firstUsableLine(text: String): String? =
        text.lineSequence()
            .map(String::trim)
            .firstOrNull(::isUsableLine)
            ?.take(64)

    fun isUsableLine(rawLine: String): Boolean {
        val line = rawLine.trim()
        return line.length in 4..64 &&
            line.any(Char::isLetter) &&
            !junkTitle.containsMatchIn(line) &&
            !isSocialCounter(line) &&
            line.lowercase().trim('!', '.', ' ') !in uiNoiseTitles &&
            line.count(Char::isDigit) < line.length / 2
    }

    /** "7 Follow", "25 likes" — social counters are not headlines. */
    private fun isSocialCounter(line: String): Boolean =
        line.lowercase().replace(lettersAndSpacesOnly, "").trim() in uiNoiseTitles
}
