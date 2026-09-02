package app.shotlist.health.api

import app.shotlist.data.GlucoseSample
import java.util.Locale
import kotlin.math.roundToInt

enum class GlucoseUnit { MMOL_PER_L, MG_PER_DL }

/** Canonical storage is mmol/L; this is the single tested conversion. */
object GlucoseUnits {
    const val MG_PER_MMOL = 18.0182

    fun defaultFor(locale: Locale): GlucoseUnit =
        if (locale.country.equals("US", ignoreCase = true)) GlucoseUnit.MG_PER_DL else GlucoseUnit.MMOL_PER_L

    fun resolve(stored: String?, locale: Locale): GlucoseUnit =
        GlucoseUnit.entries.firstOrNull { it.name == stored } ?: defaultFor(locale)

    fun toMgPerDl(mmolPerLiter: Double): Int = (mmolPerLiter * MG_PER_MMOL).roundToInt()

    fun format(mmolPerLiter: Double, unit: GlucoseUnit, locale: Locale): String = when (unit) {
        GlucoseUnit.MG_PER_DL -> String.format(locale, "%d", toMgPerDl(mmolPerLiter))
        GlucoseUnit.MMOL_PER_L -> String.format(locale, "%.1f", mmolPerLiter)
    }

    fun label(unit: GlucoseUnit): String = when (unit) {
        GlucoseUnit.MG_PER_DL -> "mg/dL"
        GlucoseUnit.MMOL_PER_L -> "mmol/L"
    }
}

/**
 * Copy and summaries for the Metabolic Lens screens. Everything is descriptive:
 * observed values, counts and gaps — no targets, zones, scores or predictions.
 */
object GlucoseStory {
    const val DAY_MS: Long = 24L * 60 * 60 * 1000
    const val LINGO_DELAY_HOURS = 3

    /** Expected cadence is five minutes; anything past this is a visible gap. */
    const val GAP_THRESHOLD_MS: Long = 30L * 60 * 1000

    enum class Freshness { RECENT, QUIET, DORMANT }

    fun freshness(latestObservedAt: Long?, now: Long): Freshness? {
        latestObservedAt ?: return null
        val age = now - latestObservedAt
        return when {
            age <= DAY_MS -> Freshness.RECENT
            age <= 14 * DAY_MS -> Freshness.QUIET
            else -> Freshness.DORMANT
        }
    }

    /**
     * The app cannot know sensor state, so stale copy never says disconnected,
     * expired or failed, and never urges a refresh.
     */
    fun freshnessCopy(freshness: Freshness?, latestTime: String, latestDate: String): String = when (freshness) {
        null -> "No readings yet."
        Freshness.RECENT ->
            "Updated through $latestTime — Lingo data can arrive about $LINGO_DELAY_HOURS hours later."
        Freshness.QUIET -> "Last reading $latestDate $latestTime. No newer sensor data has arrived."
        Freshness.DORMANT -> "Last reading $latestDate. This glucose story has not received sensor data recently."
    }

    /**
     * "Sensor glucose" is a factual claim about interstitial readings; a
     * fingerstick app or an unlabelled writer earns only "glucose".
     * No branch may ever prefix the word with blood.
     */
    fun glucoseWord(samples: List<GlucoseSample>): String =
        if (samples.isNotEmpty() && samples.all { it.specimenSource == SpecimenSource.INTERSTITIAL_FLUID.name }) {
            "sensor glucose"
        } else {
            "glucose"
        }

    data class Summary(
        val count: Int,
        val lowMmol: Double?,
        val medianMmol: Double?,
        val highMmol: Double?,
        /** Silent stretches longer than [GAP_THRESHOLD_MS] between consecutive readings. */
        val gaps: List<LongRange>,
    )

    fun summarize(samples: List<GlucoseSample>, gapThresholdMs: Long = GAP_THRESHOLD_MS): Summary {
        if (samples.isEmpty()) return Summary(0, null, null, null, emptyList())
        val sorted = samples.sortedBy { it.observedAt }
        val values = sorted.map { it.mmolPerLiter }.sorted()
        val median = if (values.size % 2 == 1) {
            values[values.size / 2]
        } else {
            (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
        }
        val gaps = sorted.zipWithNext()
            .filter { (a, b) -> b.observedAt - a.observedAt > gapThresholdMs }
            .map { (a, b) -> a.observedAt..b.observedAt }
        return Summary(values.size, values.first(), median, values.last(), gaps)
    }

    /**
     * Axis bounds come from what is on screen, padded and never narrower than
     * [minSpanMmol], so flat days do not become dramatic and no fixed range
     * pretends to be a target.
     */
    fun axisBounds(samples: List<GlucoseSample>, minSpanMmol: Double = 4.0, paddingMmol: Double = 0.5): Pair<Double, Double> {
        if (samples.isEmpty()) return 0.0 to minSpanMmol
        val low = samples.minOf { it.mmolPerLiter } - paddingMmol
        val high = samples.maxOf { it.mmolPerLiter } + paddingMmol
        val span = high - low
        if (span >= minSpanMmol) return maxOf(0.0, low) to high
        val extra = (minSpanMmol - span) / 2
        return maxOf(0.0, low - extra) to high + extra
    }
}
