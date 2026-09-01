package app.shotlist.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Dark-first liquid-glass base palette. Codex owns the full design system (t4);
// these are the scaffold defaults it will build on.
enum class ShotlistPalette(val label: String, val emoji: String) {
    COSMIC("Cosmic", "✦"),
    TIDE("Tide", "≈"),
    SUNSET("Sunset", "◉"),
}

enum class LivingScene(val label: String, val detail: String, val sceneKey: String) {
    PHASE_BEAM("Phase Beam", "Classic Android light ribbons", "phasebeam"),
    NOISE_FIELD("Noise Field", "Quiet stars drifting through glass", "noisefield"),
    FIREFLIES("Fireflies", "A warm swarm that grows with your streak", "fireflies"),
}

private val CosmicDark = darkColorScheme(
    primary = Color(0xFFA8B8FF),
    onPrimary = Color(0xFF09122E),
    primaryContainer = Color(0xFF344A86),
    onPrimaryContainer = Color(0xFFF4F6FF),
    secondary = Color(0xFF70F0D0),
    onSecondary = Color(0xFF00261F),
    secondaryContainer = Color(0xFF174C43),
    onSecondaryContainer = Color(0xFFD6FFF4),
    tertiary = Color(0xFFFF79C9),
    onTertiary = Color(0xFF34001F),
    tertiaryContainer = Color(0xFF6A2450),
    onTertiaryContainer = Color(0xFFFFE8F5),
    background = Color(0xFF090B18),
    surface = Color(0xFF12172A),
    surfaceVariant = Color(0xFF252B43),
    onBackground = Color(0xFFF4F5FF),
    onSurface = Color(0xFFF4F5FF),
    outline = Color(0xFF8E96B3),
)

private val CosmicLight = lightColorScheme(
    primary = Color(0xFF405DC4),
    secondary = Color(0xFF007D68),
    tertiary = Color(0xFFA62978),
    background = Color(0xFFF5F5FF),
    surface = Color(0xFFFFFFFF),
)

private val TideDark = darkColorScheme(
    primary = Color(0xFF62DDF5),
    onPrimary = Color(0xFF00242B),
    primaryContainer = Color(0xFF164B5A),
    secondary = Color(0xFF76F5C8),
    onSecondary = Color(0xFF00251A),
    tertiary = Color(0xFFB9A8FF),
    background = Color(0xFF06151D),
    surface = Color(0xFF102530),
    surfaceVariant = Color(0xFF1E3946),
    onBackground = Color(0xFFF0FCFF),
    onSurface = Color(0xFFF0FCFF),
    outline = Color(0xFF8BAAB4),
)

private val TideLight = lightColorScheme(
    primary = Color(0xFF00677A),
    secondary = Color(0xFF007A5C),
    tertiary = Color(0xFF5E4DB1),
    background = Color(0xFFF0FBFF),
    surface = Color(0xFFFFFFFF),
)

private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB067),
    onPrimary = Color(0xFF351A00),
    primaryContainer = Color(0xFF6E3B12),
    secondary = Color(0xFFFF7F9F),
    onSecondary = Color(0xFF38000F),
    tertiary = Color(0xFFFFD36E),
    background = Color(0xFF1B0C18),
    surface = Color(0xFF301526),
    surfaceVariant = Color(0xFF4A263A),
    onBackground = Color(0xFFFFF4F6),
    onSurface = Color(0xFFFFF4F6),
    outline = Color(0xFFC19AA9),
)

private val SunsetLight = lightColorScheme(
    primary = Color(0xFF9C4E00),
    secondary = Color(0xFFAA2D55),
    tertiary = Color(0xFF785900),
    background = Color(0xFFFFF7F4),
    surface = Color(0xFFFFFFFF),
)

private val ShotlistTypography = Typography()

@Composable
fun ShotlistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    palette: ShotlistPalette = ShotlistPalette.COSMIC,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> when (palette) {
            ShotlistPalette.COSMIC -> CosmicDark
            ShotlistPalette.TIDE -> TideDark
            ShotlistPalette.SUNSET -> SunsetDark
        }
        else -> when (palette) {
            ShotlistPalette.COSMIC -> CosmicLight
            ShotlistPalette.TIDE -> TideLight
            ShotlistPalette.SUNSET -> SunsetLight
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShotlistTypography,
        content = content,
    )
}
