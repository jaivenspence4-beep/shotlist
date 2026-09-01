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
}
