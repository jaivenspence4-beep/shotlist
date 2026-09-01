package app.shotlist.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Track › Cycle. One row per logged day. Entirely local, never backed up. */
@Entity(tableName = "cycle_entries", indices = [Index(value = ["day"], unique = true)])
data class CycleEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Local date as epoch-day (LocalDate.toEpochDay) for clean range math. */
    val day: Long,
    /** NONE, SPOTTING, LIGHT, MEDIUM, HEAVY */
    val flow: String = "NONE",
    /** Comma-joined symptom tags; free-form. */
    val symptoms: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Track › Habits. */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✅",
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

@Entity(
    tableName = "habit_ticks",
    indices = [Index(value = ["habitId", "day"], unique = true)],
)
data class HabitTick(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val day: Long,
)

/** Scan module: a camera capture that went through the pipeline. */
@Entity(tableName = "scans")
data class Scan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Links to the Shot row the capture was ingested as. */
    val shotId: Long,
    /** ANYTHING (v1); FOOD / PLANT arrive with their modules. */
    val mode: String = "ANYTHING",
    val createdAt: Long = System.currentTimeMillis(),
)
