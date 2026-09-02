package app.shotlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Health rows never leave this DAO for generic surfaces: no join to shots,
 * findings, FTS, collections, or the export/share paths that other DAOs feed.
 */
@Dao
interface GlucoseDao {
    @Upsert
    suspend fun upsertAll(rows: List<GlucoseSample>)

    @Query(
        "DELETE FROM glucose_samples WHERE sourcePackage = :origin " +
            "AND observedAt >= :from AND observedAt < :until"
    )
    suspend fun deleteWindow(origin: String, from: Long, until: Long)

    /**
     * Full reconciliation: the snapshot is the truth for [from, until), so
     * readings the user deleted in Health Connect disappear here too. Rows
     * outside the window (older than the read permission allows) are kept.
     */
    @Transaction
    suspend fun replaceWindow(origin: String, from: Long, until: Long, rows: List<GlucoseSample>) {
        deleteWindow(origin, from, until)
        if (rows.isNotEmpty()) upsertAll(rows)
    }

    /** Health Connect deletions carry no origin, so the id is matched everywhere. */
    @Query("DELETE FROM glucose_samples WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: String): Int

    @Query(
        "SELECT * FROM glucose_samples WHERE sourcePackage = :origin " +
            "AND observedAt BETWEEN :from AND :until ORDER BY observedAt ASC"
    )
    fun samplesBetween(origin: String, from: Long, until: Long): Flow<List<GlucoseSample>>

    @Query(
        "SELECT * FROM glucose_samples WHERE sourcePackage = :origin " +
            "ORDER BY observedAt DESC LIMIT 1"
    )
    fun latest(origin: String): Flow<GlucoseSample?>

    @Query("SELECT DISTINCT sourcePackage FROM glucose_samples ORDER BY sourcePackage")
    suspend fun origins(): List<String>

    @Query("SELECT COUNT(*) FROM glucose_samples")
    fun sampleCount(): Flow<Int>

    @Query("SELECT * FROM glucose_samples ORDER BY observedAt ASC")
    suspend fun allSamples(): List<GlucoseSample>

    // --- Moments ---

    @Insert
    suspend fun insertMoment(moment: GlucoseMoment): Long

    @Query("DELETE FROM glucose_moments WHERE id = :id")
    suspend fun deleteMoment(id: Long)

    @Query(
        "SELECT * FROM glucose_moments WHERE occurredAt BETWEEN :from AND :until " +
            "ORDER BY occurredAt ASC"
    )
    fun momentsBetween(from: Long, until: Long): Flow<List<GlucoseMoment>>

    @Query("SELECT * FROM glucose_moments ORDER BY occurredAt ASC")
    suspend fun allMoments(): List<GlucoseMoment>

    // --- Sync bookkeeping ---

    @Query("SELECT * FROM glucose_sync WHERE id = 1 LIMIT 1")
    suspend fun syncState(): GlucoseSyncState?

    @Query("SELECT * FROM glucose_sync WHERE id = 1 LIMIT 1")
    fun syncStateFlow(): Flow<GlucoseSyncState?>

    @Upsert
    suspend fun saveSyncState(state: GlucoseSyncState)

    @Query("DELETE FROM glucose_samples")
    suspend fun deleteAllSamples()

    @Query("DELETE FROM glucose_moments")
    suspend fun deleteAllMoments()

    @Query("DELETE FROM glucose_sync")
    suspend fun deleteSyncState()

    /** Delete All and Disconnect → Delete: every health row and all bookkeeping. */
    @Transaction
    suspend fun deleteAllHealth() {
        deleteAllSamples()
        deleteAllMoments()
        deleteSyncState()
    }
}
