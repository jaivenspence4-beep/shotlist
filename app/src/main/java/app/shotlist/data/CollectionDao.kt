package app.shotlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBoard(board: SavedBoard): Long

    @Query("SELECT id FROM collection_boards WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun boardIdByName(name: String): Long?

    @Transaction
    suspend fun getOrCreateBoard(name: String): Long {
        val clean = name.trim().replace(Regex("\\s+"), " ").take(40)
        require(clean.isNotEmpty()) { "A collection needs a name" }
        boardIdByName(clean)?.let { return it }
        val inserted = insertBoard(SavedBoard(name = clean))
        return if (inserted > 0) inserted else checkNotNull(boardIdByName(clean))
    }

    @Query(
        "SELECT boards.id AS id, boards.name AS name, boards.createdAt AS createdAt, " +
            "COUNT(pins.id) AS itemCount, MAX(pins.createdAt) AS latestPinAt " +
            "FROM collection_boards AS boards " +
            "LEFT JOIN collection_pins AS pins ON pins.boardId = boards.id " +
            "GROUP BY boards.id ORDER BY COALESCE(MAX(pins.createdAt), boards.createdAt) DESC"
    )
    fun boards(): Flow<List<BoardSummary>>

    @Query("SELECT * FROM collection_boards WHERE id = :boardId LIMIT 1")
    fun board(boardId: Long): Flow<SavedBoard?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun pin(pin: BoardPin): Long

    @Query("DELETE FROM collection_pins WHERE id = :pinId")
    suspend fun unpin(pinId: Long)

    @Query(
        "DELETE FROM collection_pins WHERE boardId = :boardId " +
            "AND targetType = :targetType AND targetId = :targetId"
    )
    suspend fun unpin(boardId: Long, targetType: String, targetId: Long)

    @Query("DELETE FROM collection_boards WHERE id = :boardId")
    suspend fun deleteBoard(boardId: Long)

    @Query(
        "SELECT boardId FROM collection_pins " +
            "WHERE targetType = :targetType AND targetId = :targetId"
    )
    fun boardIdsForTarget(targetType: String, targetId: Long): Flow<List<Long>>

    @Query(
        "SELECT pins.id AS pinId, pins.targetType AS targetType, " +
            "pins.targetId AS targetId, pins.createdAt AS pinnedAt, " +
            "CASE " +
            "WHEN findings.vaulted = 1 THEN 'Private find' " +
            "WHEN pins.targetType = 'SHOT' AND EXISTS (" +
            "SELECT 1 FROM findings AS secret WHERE secret.shotId = shots.id AND secret.vaulted = 1" +
            ") THEN 'Private screenshot' " +
            "WHEN findings.title IS NOT NULL AND TRIM(findings.title) != '' THEN findings.title " +
            "WHEN shots.ocrText IS NOT NULL AND TRIM(shots.ocrText) != '' " +
            "THEN SUBSTR(REPLACE(shots.ocrText, CHAR(10), ' '), 1, 80) " +
            "ELSE 'Saved screenshot' END AS title, " +
            "CASE WHEN findings.vaulted = 1 THEN '' ELSE COALESCE(findings.snippet, '') END AS detail, " +
            "findings.id AS findingId, shots.id AS shotId, " +
            "CASE WHEN findings.vaulted = 1 OR EXISTS (" +
            "SELECT 1 FROM findings AS secret WHERE secret.shotId = shots.id AND secret.vaulted = 1" +
            ") THEN NULL ELSE shots.uri END AS uri, " +
            "shots.takenAt AS takenAt, " +
            "CASE WHEN findings.vaulted = 1 OR EXISTS (" +
            "SELECT 1 FROM findings AS secret WHERE secret.shotId = shots.id AND secret.vaulted = 1" +
            ") THEN 1 ELSE 0 END AS privatePixels, " +
            "CASE WHEN findings.id IS NOT NULL OR shots.id IS NOT NULL THEN 1 ELSE 0 END AS available " +
            "FROM collection_pins AS pins " +
            "LEFT JOIN findings ON pins.targetType = 'FINDING' AND findings.id = pins.targetId " +
            "LEFT JOIN shots ON shots.id = CASE WHEN pins.targetType = 'SHOT' " +
            "THEN pins.targetId ELSE findings.shotId END " +
            "WHERE pins.boardId = :boardId ORDER BY pins.createdAt DESC"
    )
    fun items(boardId: Long): Flow<List<BoardItem>>
}
