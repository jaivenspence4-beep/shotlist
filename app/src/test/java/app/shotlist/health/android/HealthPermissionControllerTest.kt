package app.shotlist.health.android

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthPermissionControllerTest {
    @Test
    fun `second cancelled prompt requires settings and grant resets counter`() {
        val cancels = MemoryCancelStore()
        val requiredPermissions = setOf("read-glucose")
        val policy = HealthPermissionPromptPolicy(requiredPermissions, cancels)

        assertEquals(
            HealthPermissionDecision.DENIED,
            policy.recordResult(emptySet()),
        )
        assertEquals(
            HealthPermissionDecision.MANAGE_ACCESS_REQUIRED,
            policy.recordResult(emptySet()),
        )
        assertEquals(
            HealthPermissionDecision.GRANTED,
            policy.recordResult(requiredPermissions),
        )
        assertEquals(0, cancels.cancelCount)
    }

    private class MemoryCancelStore : PermissionCancelStore {
        override var cancelCount: Int = 0
    }
}
