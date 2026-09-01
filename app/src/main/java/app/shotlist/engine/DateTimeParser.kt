package app.shotlist.engine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Pulls the most likely intended future date+time out of OCR text.
 * Pure Kotlin (java.time only) so it runs under plain JUnit on CI.
 *
 * Future-bias: screenshots are about upcoming things. A month/day with no year
 * that already passed rolls forward a year; a bare weekday means the next one.
 */
object DateTimeParser {

    data class Parsed(
        val at: LocalDateTime,
        /** Higher when the text was more explicit. 0..1 */
        val confidence: Float,
        /** The matched substring, for card snippets. */
        val matched: String,
    )

    private val months = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11,
        "dec" to 12,
    )

    private val weekdays = mapOf(
        "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,
    )

    // "Aug 31", "August 31st, 2026"
    private val monthDay = Regex(
        "\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|" +
            "jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|" +
            "dec(?:ember)?)\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s*(\\d{4}))?\\b",
        RegexOption.IGNORE_CASE,
    )

    // "8/31", "08/31/2026", "8-31-26"
    private val numericDate =
        Regex("\\b(\\d{1,2})[/\\-](\\d{1,2})(?:[/\\-](\\d{2,4}))?\\b")

    // "8pm", "8:30 PM", "20:00"
    private val timeRe =
        Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b|\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b",
            RegexOption.IGNORE_CASE)

    private val weekdayRe = Regex(
        "\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val relativeRe =
        Regex("\\b(today|tonight|tomorrow)\\b", RegexOption.IGNORE_CASE)

    /** Date and time must sit near each other to combine: a chat's clock
     *  stamp at the top and a "Friday" three paragraphs later are unrelated
     *  (S26 field row id71). */
    private const val PROXIMITY_CHARS = 120

    fun parse(text: String, now: LocalDateTime = LocalDateTime.now()): Parsed? {
        val date = parseDate(text, now.toLocalDate())
        val time = parseTime(text)
        val near = date != null && time != null &&
            kotlin.math.abs(date.third - time.third) <= PROXIMITY_CHARS
        return when {
            near -> Parsed(
                LocalDateTime.of(date!!.first, time!!.first),
                confidence = 0.9f,
                matched = "${date.second} ${time.second}",
            )
            date != null -> Parsed(
                LocalDateTime.of(date.first, LocalTime.of(9, 0)),
                // A bare weekday is weak evidence — chat messages say
                // "friday" constantly without meaning a calendar event.
                confidence = if (date.fourth) 0.5f else 0.6f,
                matched = date.second,
            )
            time != null -> {
                // Time with no date: today if still ahead, else tomorrow.
                val base =
                    if (time.first.isAfter(now.toLocalTime())) now.toLocalDate()
                    else now.toLocalDate().plusDays(1)
                Parsed(LocalDateTime.of(base, time.first), 0.4f, time.second)
            }
            else -> null
        }
    }

    private data class DateHit(
        val first: LocalDate,
        val second: String,
        val third: Int,
        /** true when the only evidence was a bare weekday name */
        val fourth: Boolean = false,
    )

    fun toEpochMillis(parsed: Parsed, zone: ZoneId = ZoneId.systemDefault()): Long =
        parsed.at.atZone(zone).toInstant().toEpochMilli()

    private fun parseDate(text: String, today: LocalDate): DateHit? {
        relativeRe.find(text)?.let { m ->
            val d = when (m.value.lowercase(Locale.US)) {
                "tomorrow" -> today.plusDays(1)
                else -> today // today / tonight
            }
            return DateHit(d, m.value, m.range.first)
        }
        monthDay.find(text)?.let { m ->
            val month = months[m.groupValues[1].lowercase(Locale.US).take(4).trimEnd('.')]
                ?: months[m.groupValues[1].lowercase(Locale.US).take(3)]
                ?: return@let
            val day = m.groupValues[2].toIntOrNull() ?: return@let
            if (day !in 1..31) return@let
            val year = m.groupValues[3].toIntOrNull()
            var date = runCatching {
                LocalDate.of(year ?: today.year, month, day)
            }.getOrNull() ?: return@let
            if (year == null && date.isBefore(today)) date = date.plusYears(1)
            return DateHit(date, m.value, m.range.first)
        }
        numericDate.find(text)?.let { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@let
            val b = m.groupValues[2].toIntOrNull() ?: return@let
            if (a !in 1..12 || b !in 1..31) return@let
            val rawYear = m.groupValues[3].toIntOrNull()
            val year = when {
                rawYear == null -> today.year
                rawYear < 100 -> 2000 + rawYear
                else -> rawYear
            }
            var date = runCatching { LocalDate.of(year, a, b) }.getOrNull() ?: return@let
            if (rawYear == null && date.isBefore(today)) date = date.plusYears(1)
            return DateHit(date, m.value, m.range.first)
        }
        weekdayRe.find(text)?.let { m ->
            val dow = weekdays[m.value.lowercase(Locale.US)] ?: return@let
            var date = today.with(TemporalAdjusters.nextOrSame(dow))
            if (date == today) date = today.with(TemporalAdjusters.next(dow))
            return DateHit(date, m.value, m.range.first, fourth = true)
        }
        return null
    }

    private fun parseTime(text: String): Triple<LocalTime, String, Int>? {
        val m = timeRe.find(text) ?: return null
        return if (m.groupValues[3].isNotEmpty()) {
            var hour = m.groupValues[1].toIntOrNull() ?: return null
            if (hour !in 1..12) return null
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            val pm = m.groupValues[3].equals("pm", ignoreCase = true)
            if (pm && hour != 12) hour += 12
            if (!pm && hour == 12) hour = 0
            Triple(LocalTime.of(hour, minute), m.value, m.range.first)
        } else {
            val hour = m.groupValues[4].toIntOrNull() ?: return null
            val minute = m.groupValues[5].toIntOrNull() ?: return null
            Triple(LocalTime.of(hour, minute), m.value, m.range.first)
        }
    }
}
