package app.shotlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CycleEntry)

    @Query("SELECT * FROM cycle_entries ORDER BY day DESC LIMIT 370")
    fun lastYear(): Flow<List<CycleEntry>>

    @Query("SELECT * FROM cycle_entries WHERE flow != 'NONE' ORDER BY day DESC LIMIT 1")
    suspend fun latestFlowDay(): CycleEntry?

    @Query("DELETE FROM cycle_entries WHERE day = :day")
    suspend fun clearDay(day: Long)
}

@Dao
interface HabitDao {
    @Insert
    suspend fun insert(habit: Habit): Long

    @Query("UPDATE habits SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt")
    fun active(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tick(tick: HabitTick)

    @Query("DELETE FROM habit_ticks WHERE habitId = :habitId AND day = :day")
    suspend fun untick(habitId: Long, day: Long)

    @Query("SELECT * FROM habit_ticks WHERE day >= :sinceDay")
    fun ticksSince(sinceDay: Long): Flow<List<HabitTick>>
}

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(scan: Scan): Long

    @Query(
        "SELECT scans.* FROM scans JOIN shots ON shots.id = scans.shotId " +
            "ORDER BY scans.createdAt DESC LIMIT 100"
    )
    fun recent(): Flow<List<Scan>>

    /** Monotonic baseline for the daily "scan something" quest. */
    @Query("SELECT COUNT(*) FROM scans")
    fun count(): Flow<Int>
}
