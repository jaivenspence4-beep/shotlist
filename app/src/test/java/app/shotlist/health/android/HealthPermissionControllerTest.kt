package app.shotlist.health.android

import app.shotlist.health.api.ChangesPage
import app.shotlist.health.api.GlucosePage
import app.shotlist.health.api.HealthAvailability
import app.shotlist.health.api.HealthGateway
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthPermissionControllerTest {
    @Test
    fun `second cancelled prompt requires settings and grant resets counter`() {
        val cancels = MemoryCancelStore()
        val controller = HealthPermissionController(
            context = TestContext,
            gateway = NoAccessGateway,
            cancelStore = cancels,
        )

        assertEquals(
            HealthPermissionDecision.DENIED,
            controller.recordPromptResult(emptySet()),
        )
        assertEquals(
            HealthPermissionDecision.MANAGE_ACCESS_REQUIRED,
            controller.recordPromptResult(emptySet()),
        )
        assertEquals(
            HealthPermissionDecision.GRANTED,
            controller.recordPromptResult(controller.requiredPermissions),
        )
        assertEquals(0, cancels.cancelCount)
    }

    private class MemoryCancelStore : PermissionCancelStore {
        override var cancelCount: Int = 0
    }

    private object NoAccessGateway : HealthGateway {
        override suspend fun availability() = HealthAvailability.UNSUPPORTED
        override suspend fun hasReadPermission() = false
        override suspend fun origins(from: Long, until: Long) = emptyList<String>()
        override suspend fun readSnapshot(
            origin: String,
            from: Long,
            until: Long,
            pageToken: String?,
        ) = GlucosePage(emptyList(), null)
        override suspend fun changesToken(origin: String) = ""
        override suspend fun getChanges(token: String) = ChangesPage(emptyList(), "", false)
        override suspend fun revokeAll() = Unit
    }

    /** Constructor only accesses applicationContext; no Android method is called by this test. */
    private object TestContext : android.test.mock.MockContext() {
        override fun getApplicationContext() = this
    }
}
