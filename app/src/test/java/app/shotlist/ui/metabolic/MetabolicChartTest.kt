package app.shotlist.ui.metabolic

import org.junit.Assert.assertEquals
import org.junit.Test

class MetabolicChartTest {
    @Test
    fun `moment labels remain descriptive`() {
        assertEquals("Meal", momentLabel("MEAL"))
        assertEquals("Movement", momentLabel("EXERCISE"))
        assertEquals("Sleep", momentLabel("SLEEP"))
        assertEquals("Note", momentLabel("NOTE"))
        assertEquals("Note", momentLabel("unexpected"))
    }
}
