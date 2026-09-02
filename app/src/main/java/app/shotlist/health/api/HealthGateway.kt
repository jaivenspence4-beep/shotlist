package app.shotlist.health.api

/**
 * The only door between Shotlist and Health Connect. health/android implements
 * it against HealthConnectClient; ui/metabolic and the sync engine only ever
 * see this interface, so every screen can be driven by a fake in tests.
 *
 * Contract: read-only BloodGlucoseRecord, foreground only, at most the last
 * 30 days. Nothing here may write, request background/history access, or
 * touch the network.
 */
interface HealthGateway {
    suspend fun availability(): HealthAvailability

    suspend fun hasReadPermission(): Boolean

    /** Package names of every app that wrote glucose inside the window. */
    suspend fun origins(from: Long, until: Long): List<String>

    /** One page of records from a single writer, oldest first. */
    suspend fun readSnapshot(origin: String, from: Long, until: Long, pageToken: String?): GlucosePage

    /** A fresh changes token scoped to glucose records from [origin]. */
    suspend fun changesToken(origin: String): String

    /** @throws TokenExpiredException when Health Connect no longer honours [token]. */
    suspend fun getChanges(token: String): ChangesPage

    /** Best-effort: callers must proceed with local deletion even if this throws. */
    suspend fun revokeAll()
}

enum class HealthAvailability { AVAILABLE, NEEDS_UPDATE, NOT_INSTALLED, UNSUPPORTED }

/** Mirrors Health Connect's BloodGlucoseRecord.SPECIMEN_SOURCE_* constants. */
enum class SpecimenSource {
    INTERSTITIAL_FLUID, CAPILLARY_BLOOD, PLASMA, SERUM, TEARS, WHOLE_BLOOD, UNKNOWN;

    companion object {
        fun fromName(raw: String?): SpecimenSource =
            entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

data class GlucoseRecord(
    val recordId: String,
    val sourcePackage: String,
    val observedAt: Long,
    val zoneOffsetSeconds: Int?,
    val mmolPerLiter: Double,
    val specimen: SpecimenSource,
)

data class GlucosePage(
    val records: List<GlucoseRecord>,
    val nextPageToken: String?,
)

sealed interface GlucoseChange {
    data class Upsert(val record: GlucoseRecord) : GlucoseChange

    /** Health Connect deletions carry only the record id. */
    data class Delete(val recordId: String) : GlucoseChange
}

data class ChangesPage(
    val changes: List<GlucoseChange>,
    val nextToken: String,
    val hasMore: Boolean,
)

class TokenExpiredException : Exception("Health Connect changes token expired")
