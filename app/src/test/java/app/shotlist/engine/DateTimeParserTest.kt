package app.shotlist.engine

import java.time.LocalDateTime
import java.time.Month
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeParserTest {

    // A fixed "now": Monday 2026-08-31, 10:00.
    private val now = LocalDateTime.of(2026, 8, 31, 10, 0)

    @Test
    fun `month name plus day plus time parses fully`() {
        val p = DateTimeParser.parse("Doors open Sep 14 at 8:30 PM", now)!!
        assertEquals(Month.SEPTEMBER, p.at.month)
        assertEquals(14, p.at.dayOfMonth)
        assertEquals(20, p.at.hour)
        assertEquals(30, p.at.minute)
        assertTrue(p.confidence >= 0.9f)
    }

    @Test
    fun `passed month-day with no year rolls forward a year`() {
        val p = DateTimeParser.parse("March 3", now)!!
        assertEquals(2027, p.at.year)
        assertEquals(Month.MARCH, p.at.month)
    }

    @Test
    fun `numeric date with two-digit year expands`() {
        val p = DateTimeParser.parse("due 9/14/26", now)!!
        assertEquals(2026, p.at.year)
        assertEquals(Month.SEPTEMBER, p.at.month)
        assertEquals(14, p.at.dayOfMonth)
    }

    @Test
    fun `bare weekday means the next one`() {
        val p = DateTimeParser.parse("see you friday!", now)!!
        assertEquals(java.time.DayOfWeek.FRIDAY, p.at.dayOfWeek)
        assertTrue(p.at.toLocalDate().isAfter(now.toLocalDate()))
    }

    @Test
    fun `tomorrow resolves relative to now`() {
        val p = DateTimeParser.parse("pickup tomorrow 9am", now)!!
        assertEquals(1, p.at.dayOfMonth) // Sep 1
        assertEquals(9, p.at.hour)
    }

    @Test
    fun `time already passed today lands tomorrow`() {
        val p = DateTimeParser.parse("call at 8am", now)!! // now is 10:00
        assertEquals(1, p.at.dayOfMonth)
    }

    @Test
    fun `twelve pm is noon and twelve am is midnight`() {
        assertEquals(12, DateTimeParser.parse("lunch 12pm", now)!!.at.hour)
        assertEquals(0, DateTimeParser.parse("sale ends 12am tomorrow", now)!!.at.hour)
    }

    @Test
    fun `no datetime returns null`() {
        assertNull(DateTimeParser.parse("just a meme lol", now))
    }

    @Test
    fun `invalid calendar dates are skipped not crashed`() {
        assertNull(DateTimeParser.parse("ratio 2/31 something 45/99", now)?.takeIf {
            it.at.month == Month.FEBRUARY && it.at.dayOfMonth == 31
        })
        assertNotNull(DateTimeParser.parse("Feb 28 party", now))
    }
}
