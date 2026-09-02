package app.shotlist.health.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.shotlist.health.api.ChangesPage
import app.shotlist.health.api.GlucoseChange
import app.shotlist.health.api.GlucosePage
import app.shotlist.health.api.GlucoseRecord
import app.shotlist.health.api.HealthAvailability
import app.shotlist.health.api.HealthGateway
import app.shotlist.health.api.SpecimenSource
import app.shotlist.health.api.TokenExpiredException
import java.time.Instant

/** Read-only, foreground Health Connect adapter. It performs no logging. */
class HealthConnectGateway(
    context: Context,
    private val sdkStatus: () -> Int = {
        HealthConnectClient.getSdkStatus(context.applicationContext, PROVIDER_PACKAGE)
    },
    private val providerInstalled: () -> Boolean = {
        context.applicationContext.isPackageInstalled(PROVIDER_PACKAGE)
    },
    private val clientProvider: () -> HealthConnectClient = {
        HealthConnectClient.getOrCreate(context.applicationContext, PROVIDER_PACKAGE)
    },
) : HealthGateway {
    private val client: HealthConnectClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED, clientProvider)

    override suspend fun availability(): HealthAvailability =
        mapHealthAvailability(sdkStatus(), providerInstalled())

    override suspend fun hasReadPermission(): Boolean {
        if (availability() != HealthAvailability.AVAILABLE) return false
        return client.permissionController.getGrantedPermissions().containsAll(READ_PERMISSIONS)
    }

    override suspend fun origins(from: Long, until: Long): List<String> {
        val origins = linkedSetOf<String>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                glucoseRequest(from, until, origin = null, pageToken = pageToken),
            )
            response.records.mapTo(origins) { it.metadata.dataOrigin.packageName }
            pageToken = response.pageToken.takeUnless { it.isNullOrEmpty() }
        } while (pageToken != null)
        return origins.sorted()
    }

    override suspend fun readSnapshot(
        origin: String,
        from: Long,
        until: Long,
        pageToken: String?,
    ): GlucosePage {
        val response = client.readRecords(
            glucoseRequest(from, until, origin, pageToken.takeUnless { it.isNullOrEmpty() }),
        )
        return GlucosePage(
            records = response.records.map(BloodGlucoseRecord::toDomain),
            nextPageToken = response.pageToken.takeUnless { it.isNullOrEmpty() },
        )
    }

    override suspend fun changesToken(origin: String): String =
        client.getChangesToken(
            ChangesTokenRequest(
                recordTypes = setOf(BloodGlucoseRecord::class),
                dataOriginFilters = setOf(DataOrigin(origin)),
            ),
        )

    override suspend fun getChanges(token: String): ChangesPage {
        val response = client.getChanges(token)
        if (response.changesTokenExpired) throw TokenExpiredException()
        return ChangesPage(
            changes = response.changes.mapNotNull { change ->
                when (change) {
                    is DeletionChange -> GlucoseChange.Delete(change.recordId)
                    is UpsertionChange -> (change.record as? BloodGlucoseRecord)
                        ?.let { GlucoseChange.Upsert(it.toDomain()) }
                    else -> null
                }
            },
            nextToken = response.nextChangesToken,
            hasMore = response.hasMore,
        )
    }

    override suspend fun revokeAll() {
        if (availability() == HealthAvailability.AVAILABLE) {
            client.permissionController.revokeAllPermissions()
        }
    }

    private fun glucoseRequest(
        from: Long,
        until: Long,
        origin: String?,
        pageToken: String?,
    ) = ReadRecordsRequest(
        recordType = BloodGlucoseRecord::class,
        timeRangeFilter = TimeRangeFilter.between(
            Instant.ofEpochMilli(from),
            Instant.ofEpochMilli(until),
        ),
        dataOriginFilter = origin?.let { setOf(DataOrigin(it)) } ?: emptySet(),
        ascendingOrder = true,
        pageSize = PAGE_SIZE,
        pageToken = pageToken,
    )

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        private const val PAGE_SIZE = 1_000

        val READ_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        )
    }
}

internal fun mapHealthAvailability(
    sdkStatus: Int,
    providerInstalled: Boolean,
): HealthAvailability = when (sdkStatus) {
    HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
        if (providerInstalled) HealthAvailability.NEEDS_UPDATE else HealthAvailability.NOT_INSTALLED
    }
    else -> HealthAvailability.UNSUPPORTED
}

internal fun mapSpecimenSource(source: Int): SpecimenSource = when (source) {
    BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID -> SpecimenSource.INTERSTITIAL_FLUID
    BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD -> SpecimenSource.CAPILLARY_BLOOD
    BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA -> SpecimenSource.PLASMA
    BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM -> SpecimenSource.SERUM
    BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS -> SpecimenSource.TEARS
    BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD -> SpecimenSource.WHOLE_BLOOD
    else -> SpecimenSource.UNKNOWN
}

private fun BloodGlucoseRecord.toDomain() = GlucoseRecord(
    recordId = metadata.id,
    sourcePackage = metadata.dataOrigin.packageName,
    observedAt = time.toEpochMilli(),
    zoneOffsetSeconds = zoneOffset?.totalSeconds,
    mmolPerLiter = level.inMillimolesPerLiter,
    specimen = mapSpecimenSource(specimenSource),
)

private fun Context.isPackageInstalled(packageName: String): Boolean = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
