package app.shotlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(shot: Shot): Long

    @Query("UPDATE shots SET ocrText = :text, status = :status WHERE id = :id")
    suspend fun markProcessed(id: Long, text: String, status: String = "PROCESSED")

    @Query("SELECT * FROM shots WHERE mediaId = :mediaId LIMIT 1")
    suspend fun byMediaId(mediaId: Long): Shot?

    @Query("SELECT COUNT(*) FROM shots")
    fun count(): Flow<Int>

    @Query(
        "SELECT * FROM shots WHERE ocrText LIKE '%' || :q || '%' " +
            "ORDER BY takenAt DESC LIMIT 100"
    )
    fun search(q: String): Flow<List<Shot>>

    @Query(
        "SELECT shots.id AS shotId, shots.uri AS uri, shots.takenAt AS takenAt, " +
            "snippet(shots_fts, '[', ']', ' … ', 0, 22) AS excerpt, " +
            "findings.id AS findingId, findings.type AS findingType, " +
            "findings.title AS findingTitle, findings.snippet AS findingSnippet, " +
            "findings.whenAt AS findingWhenAt, findings.amountCents AS findingAmountCents, " +
            "findings.payload AS findingPayload, findings.confidence AS findingConfidence, " +
            "findings.state AS findingState, findings.vaulted AS findingVaulted, " +
            "findings.createdAt AS findingCreatedAt " +
            "FROM shots_fts JOIN shots ON shots_fts.rowid = shots.id " +
            "LEFT JOIN findings ON findings.id = (" +
            "SELECT best.id FROM findings AS best WHERE best.shotId = shots.id " +
            "AND best.state != 'DISMISSED' ORDER BY best.vaulted DESC, best.confidence DESC LIMIT 1" +
            ") WHERE shots_fts MATCH :matchQuery " +
            "ORDER BY shots.takenAt DESC LIMIT 40"
    )
    fun recall(matchQuery: String): Flow<List<RecallHit>>

    /**
     * A candidate is safe only when no finding on the screenshot was ever
     * accepted or vaulted, and every remaining meaning is dismissed/expired.
     * App-private scan captures are excluded: Android's trash confirmation can
     * only act on real MediaStore content URIs.
     */
    @Query(
        "SELECT shots.id AS shotId, shots.uri AS uri, shots.takenAt AS takenAt, " +
            "CASE WHEN shots.status = 'IGNORED' THEN 'No useful text found' " +
            "WHEN EXISTS (SELECT 1 FROM findings AS dismissed WHERE dismissed.shotId = shots.id " +
            "AND dismissed.state = 'DISMISSED') THEN 'Everything here was dismissed' " +
            "ELSE 'The plans here have expired' END AS reason " +
            "FROM shots WHERE shots.uri LIKE 'content://media/%' " +
            "AND NOT EXISTS (SELECT 1 FROM findings AS protected WHERE protected.shotId = shots.id " +
            "AND (protected.vaulted = 1 OR protected.state = 'ACCEPTED')) " +
            "AND NOT EXISTS (SELECT 1 FROM findings AS live WHERE live.shotId = shots.id " +
            "AND live.state != 'DISMISSED' AND NOT (" +
            "live.type IN ('EVENT', 'DEADLINE') AND live.whenAt IS NOT NULL AND live.whenAt < :now" +
            ")) AND (shots.status = 'IGNORED' OR " +
            "EXISTS (SELECT 1 FROM findings AS anyFinding WHERE anyFinding.shotId = shots.id)) " +
            "ORDER BY shots.takenAt DESC LIMIT 120"
    )
    suspend fun purgeCandidates(now: Long = System.currentTimeMillis()): List<PurgeCandidate>

    @Query("DELETE FROM scans WHERE shotId IN (:shotIds)")
    suspend fun deleteScansForShots(shotIds: List<Long>)

    @Query("DELETE FROM findings WHERE shotId IN (:shotIds)")
    suspend fun deleteFindingsForShots(shotIds: List<Long>)

    @Query("DELETE FROM shots WHERE id IN (:shotIds)")
    suspend fun deleteShotsById(shotIds: List<Long>)

    @Transaction
    suspend fun forgetTrashedShots(shotIds: List<Long>) {
        if (shotIds.isEmpty()) return
        deleteScansForShots(shotIds)
        deleteFindingsForShots(shotIds)
        deleteShotsById(shotIds)
    }

    /** After a suggestion purge, shots with nothing left re-enter the pipeline. */
    @Query("DELETE FROM shots WHERE id NOT IN (SELECT shotId FROM findings)")
    suspend fun purgeOrphans()
}

@Dao
interface FindingDao {
    /** Returns row ids in input order — the moment-loop notification must
     *  deep-link to the real persisted finding. */
    @Insert
    suspend fun insertAll(findings: List<Finding>): List<Long>

    @Query("SELECT * FROM findings WHERE state = 'SUGGESTED' ORDER BY createdAt DESC")
    fun inbox(): Flow<List<Finding>>

    @Query("SELECT * FROM findings WHERE state = 'ACCEPTED' AND whenAt IS NOT NULL AND whenAt > :now ORDER BY whenAt ASC")
    fun upcoming(now: Long = System.currentTimeMillis()): Flow<List<Finding>>

    @Query("UPDATE findings SET state = :state WHERE id = :id")
    suspend fun setState(id: Long, state: String)

    @Query("SELECT COUNT(*) FROM findings WHERE state = 'SUGGESTED'")
    fun suggestedCount(): Flow<Int>

    @Query("SELECT * FROM findings WHERE type IN (:types) ORDER BY createdAt DESC LIMIT 200")
    fun byTypes(types: List<String>): Flow<List<Finding>>

    /** Cross-shot dedupe: the same flyer screenshotted twice is one finding. */
    @Query(
        "SELECT COUNT(*) FROM findings WHERE type = :type AND " +
            "(title = :title OR (payload != '' AND payload = :payload)) AND " +
            "(whenAt IS :whenAt OR (whenAt IS NOT NULL AND :whenAt IS NOT NULL AND " +
            "ABS(whenAt - :whenAt) < 3600000))"
    )
    suspend fun duplicates(type: String, title: String, payload: String, whenAt: Long?): Int

    /** Engine upgraded: stored suggestions were made by an older, dumber brain. */
    @Query("DELETE FROM findings WHERE state = 'SUGGESTED'")
    suspend fun purgeSuggested()

    // --- Vault ---

    @Query("SELECT * FROM findings WHERE vaulted = 1 AND state != 'DISMISSED' ORDER BY createdAt DESC")
    fun vaulted(): Flow<List<Finding>>

    @Query("UPDATE findings SET vaulted = :vaulted WHERE id = :id")
    suspend fun setVaulted(id: Long, vaulted: Boolean)

    @Query("SELECT COUNT(*) FROM findings WHERE vaulted = 1 AND state != 'DISMISSED'")
    fun vaultedCount(): Flow<Int>
}
