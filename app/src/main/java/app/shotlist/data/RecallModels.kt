package app.shotlist.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Space-efficient external-content index over the OCR already stored in [Shot].
 * Room owns the sync triggers; writes continue to target shots, never this table.
 */
@Fts4(
    contentEntity = Shot::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
)
@Entity(tableName = "shots_fts")
data class ShotFts(
    val ocrText: String,
)

data class RecallHit(
    val shotId: Long,
    val uri: String,
    val takenAt: Long,
    val excerpt: String,
    val findingId: Long?,
    val findingType: String?,
    val findingTitle: String?,
    val findingSnippet: String?,
    val findingWhenAt: Long?,
    val findingAmountCents: Long?,
    val findingPayload: String?,
    val findingConfidence: Float?,
    val findingState: String?,
    val findingVaulted: Boolean?,
    val findingCreatedAt: Long?,
) {
    fun finding(): Finding? {
        val id = findingId ?: return null
        return Finding(
            id = id,
            shotId = shotId,
            type = findingType ?: return null,
            title = findingTitle.orEmpty(),
            snippet = findingSnippet.orEmpty(),
            whenAt = findingWhenAt,
            amountCents = findingAmountCents,
            payload = findingPayload.orEmpty(),
            confidence = findingConfidence ?: 1f,
            state = findingState ?: "SUGGESTED",
            vaulted = findingVaulted == true,
            createdAt = findingCreatedAt ?: takenAt,
        )
    }
}
