package app.shotlist.health.api

import app.shotlist.data.GlucoseSyncState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlucoseSyncTest {
    private val now = 1_800_000_000_000L
    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val gateway = FakeHealthGateway()
    private val store = FakeGlucoseStore()
    private val sync = GlucoseSync(gateway, store, clock = { now })

    @Test
    fun `first refresh auto-selects the sole writer, snapshots 30 days across pages, persists a token`() = runBlocking {
        gateway.pageSize = 2
        repeat(5) { i -> gateway.write(record("r$i", now - (i + 1) * hour)) }
        gateway.write(record("old", now - 40 * day))

        val result = sync.refresh()

        assertEquals(GlucoseSync.Result.Synced(imported = 5, deleted = 0), result)
        assertEquals("com.abbott.lingo", store.state.selectedOrigin)
        assertEquals(5, store.samples.size)
        assertFalse(store.samples.containsKey("com.abbott.lingo" to "old"))
        assertTrue(store.state.changesToken != null)
        assertEquals(now, store.state.lastSyncAt)
    }

    @Test
    fun `refreshing twice is idempotent and imports only the delta`() = runBlocking {
        gateway.write(record("a", now - hour))
        sync.refresh()

        gateway.write(record("late-backfill", now - 6 * hour))
        val second = sync.refresh()

        assertEquals(GlucoseSync.Result.Synced(imported = 1, deleted = 0), second)
        assertEquals(2, store.samples.size)
    }

    @Test
    fun `token replay honours deletions made in Health Connect`() = runBlocking {
        gateway.write(record("a", now - hour))
        gateway.write(record("b", now - 2 * hour))
        sync.refresh()

        gateway.delete("a")
        val result = sync.refresh()

        assertEquals(GlucoseSync.Result.Synced(imported = 0, deleted = 1), result)
        assertEquals(setOf("b"), store.samples.keys.map { it.second }.toSet())
    }

    @Test
    fun `deletion from selected writer cannot erase colliding id retained from another writer`() = runBlocking {
        gateway.write(record("shared-id", now - hour, origin = "com.abbott.lingo"))
        sync.refresh()
        store.samples["com.dexcom.g7" to "shared-id"] = sample(
            id = "shared-id",
            observedAt = now - 2 * hour,
            origin = "com.dexcom.g7",
        )

        gateway.delete("shared-id")
        val result = sync.refresh()

        assertEquals(GlucoseSync.Result.Synced(imported = 0, deleted = 1), result)
        assertFalse(store.samples.containsKey("com.abbott.lingo" to "shared-id"))
        assertTrue(store.samples.containsKey("com.dexcom.g7" to "shared-id"))
    }

    @Test
    fun `expired token falls back to a full reconciliation that drops deleted rows`() = runBlocking {
        gateway.write(record("a", now - hour))
        gateway.write(record("b", now - 2 * hour))
        sync.refresh()
        val stale = store.state.changesToken!!

        gateway.records.remove("a")
        gateway.expire(stale)
        val result = sync.refresh()

        assertTrue(result is GlucoseSync.Result.Synced)
        assertEquals(setOf("b"), store.samples.keys.map { it.second }.toSet())
        assertTrue(store.state.changesToken != stale)
    }

    @Test
    fun `full reconciliation keeps rows older than the readable window`() = runBlocking {
        store.samples["com.abbott.lingo" to "ancient"] = sample("ancient", now - 45 * day)
        store.state = GlucoseSyncState(selectedOrigin = "com.abbott.lingo")
        gateway.write(record("fresh", now - hour))

        sync.refresh()

        assertTrue(store.samples.containsKey("com.abbott.lingo" to "ancient"))
        assertTrue(store.samples.containsKey("com.abbott.lingo" to "fresh"))
    }

    @Test
    fun `two writers require an explicit choice and nothing is imported`() = runBlocking {
        gateway.write(record("a", now - hour, origin = "com.abbott.lingo"))
        gateway.write(record("b", now - hour, origin = "com.dexcom.g7"))

        val result = sync.refresh()

        assertEquals(GlucoseSync.Result.NeedsSource(listOf("com.abbott.lingo", "com.dexcom.g7")), result)
        assertTrue(store.samples.isEmpty())
    }

    @Test
    fun `selected writer never mixes with another writer's records`() = runBlocking {
        gateway.write(record("a", now - hour, origin = "com.abbott.lingo"))
        gateway.write(record("b", now - hour, origin = "com.dexcom.g7"))
        sync.selectSource("com.abbott.lingo")

        sync.refresh()

        assertEquals(listOf("com.abbott.lingo"), store.samples.keys.map { it.first }.distinct())
    }

    @Test
    fun `non-finite and non-positive values are dropped, everything else kept unclipped`() = runBlocking {
        gateway.write(record("nan", now - hour, mmol = Double.NaN))
        gateway.write(record("zero", now - 2 * hour, mmol = 0.0))
        gateway.write(record("neg", now - 3 * hour, mmol = -1.0))
        gateway.write(record("wayHigh", now - 4 * hour, mmol = 27.7))
        gateway.write(record("wayLow", now - 5 * hour, mmol = 1.2))

        sync.refresh()

        assertEquals(setOf("wayHigh", "wayLow"), store.samples.keys.map { it.second }.toSet())
    }

    @Test
    fun `specimen source survives import so copy can be honest`() = runBlocking {
        gateway.write(record("stick", now - hour, specimen = SpecimenSource.CAPILLARY_BLOOD))

        sync.refresh()

        assertEquals("CAPILLARY_BLOOD", store.samples.values.single().specimenSource)
    }

    @Test
    fun `paused, unavailable and unpermitted states never touch the provider data`() = runBlocking {
        gateway.write(record("a", now - hour))

        store.state = GlucoseSyncState(paused = true)
        assertEquals(GlucoseSync.Result.Paused, sync.refresh())

        store.state = GlucoseSyncState()
        gateway.availability = HealthAvailability.NOT_INSTALLED
        assertEquals(GlucoseSync.Result.Unavailable(HealthAvailability.NOT_INSTALLED), sync.refresh())

        gateway.availability = HealthAvailability.AVAILABLE
        gateway.permitted = false
        assertEquals(GlucoseSync.Result.NoAccess, sync.refresh())

        assertTrue(store.samples.isEmpty())
    }

    @Test
    fun `no writer at all is the empty state, not an error`() = runBlocking {
        assertEquals(GlucoseSync.Result.NoSource, sync.refresh())
        assertNull(store.state.selectedOrigin)
    }

    @Test
    fun `disconnect keeping history revokes, clears the token, keeps rows and origin`() = runBlocking {
        gateway.write(record("a", now - hour))
        sync.refresh()

        val revoked = sync.disconnect(keepHistory = true)

        assertTrue(revoked)
        assertEquals(1, gateway.revokeCalls)
        assertNull(store.state.changesToken)
        assertEquals("com.abbott.lingo", store.state.selectedOrigin)
        assertEquals(1, store.samples.size)
    }

    @Test
    fun `disconnect deleting history wipes everything even when revocation fails`() = runBlocking {
        gateway.write(record("a", now - hour))
        sync.refresh()
        gateway.revokeThrows = true

        val revoked = sync.disconnect(keepHistory = false)

        assertFalse(revoked)
        assertTrue(store.samples.isEmpty())
        assertTrue(store.momentsWiped)
        assertEquals(GlucoseSyncState(), store.state)
    }

    @Test
    fun `delete everything wipes before attempting revocation`() = runBlocking {
        gateway.write(record("a", now - hour))
        sync.refresh()
        gateway.revokeThrows = true

        sync.deleteEverything()

        assertTrue(store.samples.isEmpty())
        assertEquals(GlucoseSyncState(), store.state)
    }

    @Test
    fun `changing source forces a fresh snapshot on the next refresh`() = runBlocking {
        gateway.write(record("a", now - hour))
        sync.refresh()

        sync.selectSource("com.dexcom.g7")

        assertNull(store.state.changesToken)
        assertEquals("com.dexcom.g7", store.state.selectedOrigin)
    }
}
