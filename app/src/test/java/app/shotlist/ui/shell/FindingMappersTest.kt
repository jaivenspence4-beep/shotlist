package app.shotlist.ui.shell

import app.shotlist.data.Finding
import org.junit.Assert.assertEquals
import org.junit.Test

class FindingMappersTest {
    @Test
    fun `vaulted note hides title and detail`() {
        val action = note(vaulted = true).toShotlistAction()

        assertEquals("Vaulted note", action.title)
        assertEquals("Unlock to read.", action.detail)
    }

    @Test
    fun `unvaulted note remains readable`() {
        val action = note(vaulted = false).toShotlistAction()

        assertEquals("Secret appointment", action.title)
        assertEquals("Secret appointment\nBring documents", action.detail)
    }

    private fun note(vaulted: Boolean) = Finding(
        id = 7,
        shotId = 11,
        type = "NOTE",
        title = "Secret appointment",
        snippet = "Secret appointment\nBring documents",
        payload = "Secret appointment\nBring documents",
        confidence = 1f,
        vaulted = vaulted,
    )
}
