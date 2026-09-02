package app.shotlist.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_boards",
    indices = [Index(value = ["name"], unique = true)],
)
data class SavedBoard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "collection_pins",
    foreignKeys = [
        ForeignKey(
            entity = SavedBoard::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("boardId"),
        Index(value = ["boardId", "targetType", "targetId"], unique = true),
    ],
)
data class BoardPin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boardId: Long,
    /** FINDING or SHOT. Kept polymorphic so one board can hold either. */
    val targetType: String,
    val targetId: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

object CollectionTargetType {
    const val FINDING = "FINDING"
    const val SHOT = "SHOT"
}

data class BoardSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val itemCount: Int,
    val latestPinAt: Long?,
)

/** Flattened projection for a board; raw private text never leaves SQL. */
data class BoardItem(
    val pinId: Long,
    val targetType: String,
    val targetId: Long,
    val pinnedAt: Long,
    val title: String,
    val detail: String,
    val findingId: Long?,
    val shotId: Long?,
    val uri: String?,
    val takenAt: Long?,
    val privatePixels: Boolean,
    val available: Boolean,
)
