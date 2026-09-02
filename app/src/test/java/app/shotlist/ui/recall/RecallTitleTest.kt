package app.shotlist.ui.recall

import org.junit.Assert.assertEquals
import org.junit.Test

class RecallTitleTest {
    @Test
    fun `stale url finding title falls through to clean excerpt line`() {
        assertEquals(
            "Open house this Sunday",
            recallHeaderTitle(
                findingTitle = "% zillow.com/homedet",
                excerpt = "% zillow.com/homedet\nOpen house this Sunday",
            ),
        )
    }

    @Test
    fun `fts match markers never leak into header`() {
        assertEquals(
            "Summer Nights Festival",
            recallHeaderTitle(null, "[Summer] Nights Festival\nDoors at 7"),
        )
    }

    @Test
    fun `social counter falls through to meaningful line`() {
        assertEquals(
            "Tour dates announced",
            recallHeaderTitle(null, "7 Follow\nTour dates announced"),
        )
    }

    @Test
    fun `all junk uses stable fallback`() {
        assertEquals(
            "Screenshot match",
            recallHeaderTitle("www.example.com", "% zillow.com/homedet\n25 likes"),
        )
    }

    @Test
    fun `valid finding title wins over excerpt`() {
        assertEquals(
            "Dinner reservation",
            recallHeaderTitle("Dinner reservation", "Tour dates announced"),
        )
    }
}
