package app.shotlist.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One ingested screenshot. */
@Entity(tableName = "shots", indices = [Index(value = ["mediaId"], unique = true)])
data class Shot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val uri: String,
    val takenAt: Long,
    val ocrText: String = "",
    /** NEW → PROCESSED → (IGNORED when classified as pure noise). */
    val status: String = "NEW",
    val createdAt: Long = System.currentTimeMillis(),
)

/** One actionable thing extracted from a shot. */
@Entity(tableName = "findings", indices = [Index("shotId"), Index("state")])
data class Finding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shotId: Long,
    /** EVENT, DEADLINE, PRODUCT, PLACE, CODE, WIFI, URL, PHONE, TRACKING, RECIPE, MEME, NOISE */
    val type: String,
    val title: String,
    val snippet: String = "",
    /** Epoch millis for events/deadlines; null otherwise. */
    val whenAt: Long? = null,
    /** Cents, for products. */
    val amountCents: Long? = null,
    /** Type-specific extra (a code, an SSID, a url…). */
    val payload: String = "",
    val confidence: Float,
    /** SUGGESTED → ACCEPTED / SNOOZED / DISMISSED */
    val state: String = "SUGGESTED",
    /** Vaulted findings render masked and require biometric to reveal. */
    val vaulted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
