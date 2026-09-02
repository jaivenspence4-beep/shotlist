package app.shotlist.health.android

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodGlucoseRecord
import app.shotlist.health.api.HealthAvailability
import app.shotlist.health.api.SpecimenSource
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectGatewayTest {
    @Test
    fun `available SDK maps to available`() {
        assertEquals(
            HealthAvailability.AVAILABLE,
            mapHealthAvailability(HealthConnectClient.SDK_AVAILABLE, providerInstalled = true),
        )
    }

    @Test
    fun `missing provider is distinguished from provider update`() {
        assertEquals(
            HealthAvailability.NOT_INSTALLED,
            mapHealthAvailability(
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
                providerInstalled = false,
            ),
        )
        assertEquals(
            HealthAvailability.NEEDS_UPDATE,
            mapHealthAvailability(
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
                providerInstalled = true,
            ),
        )
    }

    @Test
    fun `unknown SDK status is unsupported`() {
        assertEquals(
            HealthAvailability.UNSUPPORTED,
            mapHealthAvailability(Int.MIN_VALUE, providerInstalled = true),
        )
    }

    @Test
    fun `every Health Connect specimen constant has a domain mapping`() {
        val mappings = mapOf(
            BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID to SpecimenSource.INTERSTITIAL_FLUID,
            BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD to SpecimenSource.CAPILLARY_BLOOD,
            BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA to SpecimenSource.PLASMA,
            BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM to SpecimenSource.SERUM,
            BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS to SpecimenSource.TEARS,
            BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD to SpecimenSource.WHOLE_BLOOD,
            BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN to SpecimenSource.UNKNOWN,
        )

        mappings.forEach { (raw, expected) ->
            assertEquals(expected, mapSpecimenSource(raw))
        }
        assertEquals(SpecimenSource.UNKNOWN, mapSpecimenSource(Int.MAX_VALUE))
    }
}
