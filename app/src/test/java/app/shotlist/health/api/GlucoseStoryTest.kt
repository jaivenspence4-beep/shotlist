package app.shotlist.health.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GlucoseStoryTest {
    private val now = 1_800_000_000_000L
    private val minute = 60L * 1000
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun `mg per dL rounds to the nearest whole number`() {
        assertEquals(99, GlucoseUnits.toMgPerDl(5.5))
        assertEquals(70, GlucoseUnits.toMgPerDl(3.9))
        assertEquals(180, GlucoseUnits.toMgPerDl(10.0))
        assertEquals(56, GlucoseUnits.toMgPerDl(3.1))
        assertEquals(200, GlucoseUnits.toMgPerDl(11.1))
    }

    @Test
    fun `formatting follows the unit and the locale`() {
        assertEquals("99", GlucoseUnits.format(5.5, GlucoseUnit.MG_PER_DL, Locale.US))
        assertEquals("5.5", GlucoseUnits.format(5.5, GlucoseUnit.MMOL_PER_L, Locale.US))
        assertEquals("5,5", GlucoseUnits.format(5.5, GlucoseUnit.MMOL_PER_L, Locale.GERMANY))
    }

    @Test
    fun `US defaults to mg per dL, everyone else to mmol, stored preference wins`() {
        assertEquals(GlucoseUnit.MG_PER_DL, GlucoseUnits.defaultFor(Locale.US))
        assertEquals(GlucoseUnit.MMOL_PER_L, GlucoseUnits.defaultFor(Locale.UK))
        assertEquals(GlucoseUnit.MMOL_PER_L, GlucoseUnits.defaultFor(Locale.CANADA))
        assertEquals(GlucoseUnit.MMOL_PER_L, GlucoseUnits.resolve("MMOL_PER_L", Locale.US))
        assertEquals(GlucoseUnit.MG_PER_DL, GlucoseUnits.resolve("garbage", Locale.US))
        assertEquals(GlucoseUnit.MMOL_PER_L, GlucoseUnits.resolve(null, Locale.FRANCE))
    }

    @Test
    fun `freshness tiers at 24 hours and 14 days`() {
        assertNull(GlucoseStory.freshness(null, now))
        assertEquals(GlucoseStory.Freshness.RECENT, GlucoseStory.freshness(now - 3 * hour, now))
        assertEquals(GlucoseStory.Freshness.RECENT, GlucoseStory.freshness(now - day, now))
        assertEquals(GlucoseStory.Freshness.QUIET, GlucoseStory.freshness(now - day - 1, now))
        assertEquals(GlucoseStory.Freshness.QUIET, GlucoseStory.freshness(now - 14 * day, now))
        assertEquals(GlucoseStory.Freshness.DORMANT, GlucoseStory.freshness(now - 14 * day - 1, now))
    }

    @Test
    fun `stale copy never claims to know the sensor state`() {
        GlucoseStory.Freshness.entries.forEach { tier ->
            val copy = GlucoseStory.freshnessCopy(tier, "14:05", "12 Aug").lowercase()
            listOf("disconnected", "expired", "failed", "refresh", "live", "now", "blood").forEach { banned ->
                assertFalse("'$banned' in $tier copy", copy.contains(banned))
            }
        }
        assertEquals("No readings yet.", GlucoseStory.freshnessCopy(null, "", ""))
        assertTrue(GlucoseStory.freshnessCopy(GlucoseStory.Freshness.RECENT, "14:05", "").contains("3 hours later"))
    }

    @Test
    fun `sensor wording needs every displayed sample to be interstitial`() {
        val interstitial = listOf(sample("a", now), sample("b", now - hour))
        assertEquals("sensor glucose", GlucoseStory.glucoseWord(interstitial))

        val mixed = interstitial + sample("c", now - 2 * hour, specimen = SpecimenSource.CAPILLARY_BLOOD)
        assertEquals("glucose", GlucoseStory.glucoseWord(mixed))

        val unknown = listOf(sample("d", now, specimen = SpecimenSource.UNKNOWN))
        assertEquals("glucose", GlucoseStory.glucoseWord(unknown))
        assertEquals("glucose", GlucoseStory.glucoseWord(emptyList()))
    }

    @Test
    fun `summary reports observed low, median, high, count and gaps only`() {
        val samples = listOf(
            sample("a", now - 50 * minute, mmol = 6.1),
            sample("b", now - 45 * minute, mmol = 4.2),
            sample("c", now - 40 * minute, mmol = 9.8),
            sample("d", now - 5 * minute, mmol = 5.0),
        )

        val summary = GlucoseStory.summarize(samples)

        assertEquals(4, summary.count)
        assertEquals(4.2, summary.lowMmol!!, 1e-9)
        assertEquals(5.55, summary.medianMmol!!, 1e-9)
        assertEquals(9.8, summary.highMmol!!, 1e-9)
        assertEquals(listOf((now - 40 * minute)..(now - 5 * minute)), summary.gaps)
    }

    @Test
    fun `empty summary has no numbers to misread`() {
        val summary = GlucoseStory.summarize(emptyList())
        assertEquals(0, summary.count)
        assertNull(summary.lowMmol)
        assertTrue(summary.gaps.isEmpty())
    }

    @Test
    fun `axis follows the visible data with a minimum span and never a fixed band`() {
        val flat = listOf(sample("a", now, mmol = 5.4), sample("b", now - hour, mmol = 5.6))
        val (low, high) = GlucoseStory.axisBounds(flat)
        assertEquals(4.0, high - low, 1e-9)
        assertTrue(low <= 5.4 - 0.5 && high >= 5.6 + 0.5)

        val wide = listOf(sample("a", now, mmol = 2.0), sample("b", now - hour, mmol = 20.0))
        val (wLow, wHigh) = GlucoseStory.axisBounds(wide)
        assertEquals(1.5, wLow, 1e-9)
        assertEquals(20.5, wHigh, 1e-9)

        val (eLow, eHigh) = GlucoseStory.axisBounds(emptyList())
        assertEquals(0.0, eLow, 1e-9)
        assertEquals(4.0, eHigh, 1e-9)
    }

    @Test
    fun `specimen names round-trip and unknown strings degrade safely`() {
        assertEquals(SpecimenSource.INTERSTITIAL_FLUID, SpecimenSource.fromName("INTERSTITIAL_FLUID"))
        assertEquals(SpecimenSource.UNKNOWN, SpecimenSource.fromName("plasma"))
        assertEquals(SpecimenSource.UNKNOWN, SpecimenSource.fromName(null))
    }
}
