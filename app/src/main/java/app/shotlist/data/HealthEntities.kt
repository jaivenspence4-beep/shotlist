package app.shotlist.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Track › Metabolic Lens. One sensor-glucose reading copied from Health
 * Connect. Health rows are the strictest data tier in the app: never joined
 * to shots/findings, never indexed, never surfaced outside ui/metabolic.
 * The composite key mirrors Health Connect identity (record ids are only
 * guaranteed unique per writing app).
 */
@Entity(
    tableName = "glucose_samples",
    primaryKeys = ["sourcePackage", "recordId"],
    indices = [Index("observedAt")],
)
data class GlucoseSample(
    val sourcePackage: String,
    val recordId: String,
    /** Epoch millis of the reading itself, not of its (delayed) arrival. */
    val observedAt: Long,
    /** Writer's zone offset when known; null means "treat as device zone". */
    val zoneOffsetSeconds: Int?,
    /** Canonical unit. Display conversion happens in health/api. */
    val mmolPerLiter: Double,
    /** INTERSTITIAL_FLUID, CAPILLARY_BLOOD, PLASMA, SERUM, TEARS, WHOLE_BLOOD, UNKNOWN */
    val specimenSource: String,
    val importedAt: Long,
)

/** A user-labelled point in time laid over the curve. Never implies causation. */
@Entity(tableName = "glucose_moments", indices = [Index("occurredAt")])
data class GlucoseMoment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAt: Long,
    /** MEAL, EXERCISE, SLEEP, NOTE */
    val kind: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Single-row sync bookkeeping (id is always 1). Lives in the database rather
 * than prefs so Delete All wipes it in the same transaction as the samples.
 */
@Entity(tableName = "glucose_sync")
data class GlucoseSyncState(
    @PrimaryKey val id: Int = 1,
    /** Package name of the one Health Connect writer the user chose. */
    val selectedOrigin: String? = null,
    /** Health Connect changes token; null forces a full 30-day reconciliation. */
    val changesToken: String? = null,
    val lastSyncAt: Long? = null,
    val paused: Boolean = false,
    /** MMOL_PER_L or MG_PER_DL; null means locale default. */
    val displayUnit: String? = null,
)
