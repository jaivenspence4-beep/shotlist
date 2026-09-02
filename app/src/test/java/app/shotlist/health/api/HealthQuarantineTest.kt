package app.shotlist.health.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Metabolic Lens data lives in the strictest tier: nothing outside the health
 * module and its three mount points may even name a glucose type or table.
 * A new reference in the engine, widgets, notifications, diagnostics or any
 * other screen fails here before it can ship.
 */
class HealthQuarantineTest {
    private val sourceRoot: File = listOf("src/main/java/app/shotlist", "app/src/main/java/app/shotlist")
        .map(::File)
        .first { it.isDirectory }

    private val healthSymbols = Regex(
        "Glucose|glucose_|HealthGateway|HealthConnect|health\\.api|health\\.android|Metabolic|BloodGlucose",
    )

    /** Files that are allowed to know health data exists, relative to the source root. */
    private val quarantine = listOf(
        "health/",
        "ui/metabolic/",
        "data/HealthEntities.kt",
        "data/HealthDao.kt",
        "data/ShotlistDb.kt",
        "data/ShotlistExport.kt",
        "ui/shell/AppShell.kt",
        "ui/track/TrackScreen.kt",
        "ui/you/YouScreen.kt",
    )

    @Test
    fun `no source outside the quarantine names health data`() {
        val leaks = kotlinFiles()
            .filterNot { file -> quarantine.any { relative(file).startsWith(it) } }
            .filter { file -> healthSymbols.containsMatchIn(file.readText()) }
            .map(::relative)
        assertEquals("Health references outside the quarantine", emptyList<String>(), leaks)
    }

    @Test
    fun `the quarantine really is where health lives`() {
        assertTrue(sourceRoot.resolve("health/api/GlucoseSync.kt").isFile)
        assertTrue(sourceRoot.resolve("data/HealthDao.kt").isFile)
    }

    @Test
    fun `health code never logs and never writes diagnostics`() {
        val loggers = Regex("android\\.util\\.Log|\\bLog\\.[diewv]\\(|\\bDiag\\.|println\\(|printStackTrace\\(")
        val offenders = kotlinFiles()
            .filter { relative(it).startsWith("health/") || relative(it).startsWith("ui/metabolic/") }
            .filter { loggers.containsMatchIn(it.readText()) }
            .map(::relative)
        assertEquals("Logging inside health code", emptyList<String>(), offenders)
    }

    @Test
    fun `health screens are never gated by entitlement tier`() {
        val tierSymbols = Regex("Entitlement|isPro\\b|ProPreview|paywall|BILLING_LIVE")
        val offenders = kotlinFiles()
            .filter { relative(it).startsWith("health/") || relative(it).startsWith("ui/metabolic/") }
            .filter { tierSymbols.containsMatchIn(it.readText()) }
            .map(::relative)
        assertEquals("Tier gating inside health code", emptyList<String>(), offenders)
    }

    @Test
    fun `health code never reaches the engine, widgets, notifications or share surfaces`() {
        val outbound = Regex(
            "app\\.shotlist\\.engine|app\\.shotlist\\.widget|app\\.shotlist\\.diag|" +
                "NotificationManager|NotificationCompat|ui\\.share|ui\\.quests|ui\\.memories|ui\\.collections",
        )
        val offenders = kotlinFiles()
            .filter { relative(it).startsWith("health/") || relative(it).startsWith("ui/metabolic/") }
            .filter { outbound.containsMatchIn(it.readText()) }
            .map(::relative)
        assertEquals("Health code reaching shared surfaces", emptyList<String>(), offenders)
    }

    @Test
    fun `health copy never says blood glucose or promises live data`() {
        val banned = Regex("(?i)blood glucose|real[- ]time|live glucose|\\balerts?\\b|dosing|diagnos")
        val stringLiteral = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
        val offenders = kotlinFiles()
            .filter { relative(it).startsWith("ui/metabolic/") || relative(it).startsWith("health/") }
            .flatMap { file ->
                stringLiteral.findAll(file.readText())
                    .map { it.value }
                    .filter { banned.containsMatchIn(it) }
                    .map { "${relative(file)}: $it" }
                    .toList()
            }
        assertEquals("Forbidden health copy", emptyList<String>(), offenders)
    }

    private fun kotlinFiles(): List<File> =
        sourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun relative(file: File): String =
        file.relativeTo(sourceRoot).path.replace(File.separatorChar, '/')
}
