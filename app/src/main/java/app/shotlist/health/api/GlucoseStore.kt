package app.shotlist.health.api

import app.shotlist.data.GlucoseDao
import app.shotlist.data.GlucoseSample
import app.shotlist.data.GlucoseSyncState

/** What the sync engine needs from persistence; the Room DAO satisfies it. */
interface GlucoseStore {
    suspend fun syncState(): GlucoseSyncState
    suspend fun saveSyncState(state: GlucoseSyncState)
    suspend fun replaceWindow(origin: String, from: Long, until: Long, rows: List<GlucoseSample>)
    suspend fun upsertAll(rows: List<GlucoseSample>)
    suspend fun deleteByRecordId(origin: String, recordId: String): Int
    suspend fun deleteAllHealth()
}

class RoomGlucoseStore(private val dao: GlucoseDao) : GlucoseStore {
    override suspend fun syncState(): GlucoseSyncState = dao.syncState() ?: GlucoseSyncState()
    override suspend fun saveSyncState(state: GlucoseSyncState) = dao.saveSyncState(state)
    override suspend fun replaceWindow(origin: String, from: Long, until: Long, rows: List<GlucoseSample>) =
        dao.replaceWindow(origin, from, until, rows)
    override suspend fun upsertAll(rows: List<GlucoseSample>) = dao.upsertAll(rows)
    override suspend fun deleteByRecordId(origin: String, recordId: String): Int =
        dao.deleteByRecordId(origin, recordId)
    override suspend fun deleteAllHealth() = dao.deleteAllHealth()
}
