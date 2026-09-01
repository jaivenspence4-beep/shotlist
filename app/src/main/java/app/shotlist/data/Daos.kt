package app.shotlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    /** After a suggestion purge, shots with nothing left re-enter the pipeline. */
    @Query("DELETE FROM shots WHERE id NOT IN (SELECT shotId FROM findings)")
    suspend fun purgeOrphans()
}

@Dao
interface FindingDao {
    @Insert
    suspend fun insertAll(findings: List<Finding>)

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
}
