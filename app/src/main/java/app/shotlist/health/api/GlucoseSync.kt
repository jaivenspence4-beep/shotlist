package app.shotlist.health.api

import app.shotlist.data.GlucoseSample

/**
 * Pure-Kotlin sync engine for Metabolic Lens. Runs only when the user is on
 * the screen (the UI calls [refresh]); never scheduled, never in background.
 *
 * Why change tokens instead of "everything after my newest reading": Lingo
 * writes about three hours late and a reconnected sensor backfills OLDER
 * readings out of order, so a high-water mark silently loses data. The token
 * is acquired BEFORE the snapshot so anything racing the snapshot is replayed.
 *
 * Deliberately no logging: a health value or timestamp in logcat is a leak.
 */
class GlucoseSync(
    private val gateway: HealthGateway,
    private val store: GlucoseStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val windowMillis: Long = WINDOW_30_DAYS,
) {
    sealed interface Result {
        data object Paused : Result
        data class Unavailable(val availability: HealthAvailability) : Result
        data object NoAccess : Result
        /** Nothing has written glucose yet; the empty-state copy applies. */
        data object NoSource : Result
        /** More than one app writes glucose; the user must pick, never a silent mix. */
        data class NeedsSource(val origins: List<String>) : Result
        data class Synced(val imported: Int, val deleted: Int) : Result
        /** Coarse code only — never a message carrying data. */
        data class Failed(val code: String) : Result
    }

    suspend fun refresh(): Result {
        val state = store.syncState()
        if (state.paused) return Result.Paused
        val availability = gateway.availability()
        if (availability != HealthAvailability.AVAILABLE) return Result.Unavailable(availability)
        if (!gateway.hasReadPermission()) return Result.NoAccess

        val until = clock()
        val from = until - windowMillis
        val origin = state.selectedOrigin ?: run {
            val origins = gateway.origins(from, until)
            when (origins.size) {
                0 -> return Result.NoSource
                1 -> origins.single().also { store.saveSyncState(state.copy(selectedOrigin = it)) }
                else -> return Result.NeedsSource(origins)
            }
        }

        val token = state.changesToken ?: return fullReconcile(origin, from, until)
        return try {
            drain(origin, token, imported = 0)
        } catch (expired: TokenExpiredException) {
            fullReconcile(origin, from, until)
        }
    }

    /** Switching writers invalidates the token; the next refresh re-snapshots. */
    suspend fun selectSource(origin: String) {
        val state = store.syncState()
        store.saveSyncState(state.copy(selectedOrigin = origin, changesToken = null))
    }

    suspend fun setPaused(paused: Boolean) {
        store.saveSyncState(store.syncState().copy(paused = paused))
    }

    suspend fun setDisplayUnit(unit: GlucoseUnit?) {
        store.saveSyncState(store.syncState().copy(displayUnit = unit?.name))
    }

    /**
     * Both Disconnect choices revoke Health Connect access; only row retention
     * differs. Revocation is best-effort and must never block local deletion.
     * Keeping history keeps the origin too — it is the key the story is read by.
     */
    suspend fun disconnect(keepHistory: Boolean): Boolean {
        val revoked = runCatching { gateway.revokeAll() }.isSuccess
        if (keepHistory) {
            val state = store.syncState()
            store.saveSyncState(state.copy(changesToken = null, lastSyncAt = null, paused = false))
        } else {
            store.deleteAllHealth()
        }
        return revoked
    }

    /** Delete All: wipe first, then try to revoke so a provider failure cannot keep data. */
    suspend fun deleteEverything(): Boolean {
        store.deleteAllHealth()
        return runCatching { gateway.revokeAll() }.isSuccess
    }

    private suspend fun fullReconcile(origin: String, from: Long, until: Long): Result {
        val token = gateway.changesToken(origin)
        val rows = ArrayList<GlucoseSample>()
        var page: String? = null
        do {
            val snapshot = gateway.readSnapshot(origin, from, until, page)
            snapshot.records.forEach { record -> record.toSample(origin)?.let(rows::add) }
            page = snapshot.nextPageToken
        } while (page != null)
        store.replaceWindow(origin, from, until, rows)
        return try {
            drain(origin, token, imported = rows.size)
        } catch (expired: TokenExpiredException) {
            Result.Failed("token")
        }
    }

    /**
     * Each page's token is persisted only after that page is applied, so a
     * crash mid-drain replays at most one page and never skips one.
     */
    private suspend fun drain(origin: String, token: String, imported: Int): Result {
        var next = token
        var importedCount = imported
        var deletedCount = 0
        var hasMore: Boolean
        do {
            val page = gateway.getChanges(next)
            val upserts = page.changes
                .filterIsInstance<GlucoseChange.Upsert>()
                .mapNotNull { it.record.toSample(origin) }
            if (upserts.isNotEmpty()) store.upsertAll(upserts)
            importedCount += upserts.size
            page.changes.filterIsInstance<GlucoseChange.Delete>().forEach { change ->
                deletedCount += store.deleteByRecordId(change.recordId)
            }
            next = page.nextToken
            store.saveSyncState(store.syncState().copy(changesToken = next, lastSyncAt = clock()))
            hasMore = page.hasMore
        } while (hasMore)
        return Result.Synced(importedCount, deletedCount)
    }

    /** Every finite positive value is kept — the Abbott range is never a filter. */
    private fun GlucoseRecord.toSample(origin: String): GlucoseSample? {
        if (sourcePackage != origin) return null
        if (!mmolPerLiter.isFinite() || mmolPerLiter <= 0.0) return null
        return GlucoseSample(
            sourcePackage = sourcePackage,
            recordId = recordId,
            observedAt = observedAt,
            zoneOffsetSeconds = zoneOffsetSeconds,
            mmolPerLiter = mmolPerLiter,
            specimenSource = specimen.name,
            importedAt = clock(),
        )
    }

    companion object {
        const val WINDOW_30_DAYS: Long = 30L * 24 * 60 * 60 * 1000
    }
}
