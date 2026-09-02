package app.shotlist.ui.metabolic

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetabolicArchitectureTest {
    private val sourceDir = listOf(
        File("src/main/java/app/shotlist/ui/metabolic"),
        File("app/src/main/java/app/shotlist/ui/metabolic"),
    ).first(File::isDirectory)

    @Test
    fun `health screens never import the platform adapter`() {
        val offenders = kotlinSources()
            .filter { it.readText().contains("import androidx.health.connect") }
            .map(File::getName)
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `only secure wrappers create separate windows`() {
        val rawSurface = Regex("\\b(ModalBottomSheet|Dialog|AlertDialog)\\s*\\(")
        val offenders = kotlinSources()
            .filterNot { it.name == "SecureMetabolicSurfaces.kt" }
            .filter { rawSurface.containsMatchIn(it.readText()) }
            .map(File::getName)
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `sheet and dialog wrappers force secure windows`() {
        val wrappers = sourceDir.resolve("SecureMetabolicSurfaces.kt").readText()
        assertTrue(wrappers.contains("ModalBottomSheetProperties"))
        assertTrue(wrappers.contains("DialogProperties"))
        assertEquals(2, Regex("SecureFlagPolicy\\.SecureOn").findAll(wrappers).count())
    }

    @Test
    fun `route secures the activity window`() {
        val route = sourceDir.resolve("MetabolicLensRoute.kt").readText()
        assertTrue(route.contains("WindowManager.LayoutParams.FLAG_SECURE"))
        assertTrue(route.contains("DisposableEffect(activity)"))
    }

    private fun kotlinSources(): List<File> =
        sourceDir.listFiles().orEmpty().filter { it.extension == "kt" }
}
