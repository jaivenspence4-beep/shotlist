package app.shotlist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotlistExportTest {
    private val schema = listOf(
        "android_metadata",
        "collections",
        "findings",
        "glucose_moments",
        "glucose_samples",
        "glucose_sync",
        "habits",
        "room_master_table",
        "shots",
        "shots_fts",
        "shots_fts_config",
        "sqlite_sequence",
    )

    @Test
    fun `standard export never carries a health table`() {
        val tables = ShotlistExport.exportableTables(schema)
        assertEquals(listOf("collections", "findings", "habits", "shots"), tables)
        assertTrue(tables.none(ShotlistExport::isHealthTable))
    }

    @Test
    fun `every health table is recognised by prefix, including future ones`() {
        assertTrue(ShotlistExport.isHealthTable("glucose_samples"))
        assertTrue(ShotlistExport.isHealthTable("glucose_moments"))
        assertTrue(ShotlistExport.isHealthTable("glucose_sync"))
        assertTrue(ShotlistExport.isHealthTable("glucose_anything_new"))
        assertFalse(ShotlistExport.isHealthTable("habits"))
    }

    @Test
    fun `health files are named so a reader cannot mistake them for app data`() {
        assertEquals("glucose_samples.json", ShotlistExport.HEALTH_SAMPLES_FILE)
        assertEquals("glucose_moments.json", ShotlistExport.HEALTH_MOMENTS_FILE)
    }
}
