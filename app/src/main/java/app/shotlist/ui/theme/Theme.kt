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
private val GlassDark = darkColorScheme(
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

private val GlassLight = lightColorScheme(
    primary = Color(0xFF405DC4),
    secondary = Color(0xFF007D68),
    tertiary = Color(0xFFA62978),
    background = Color(0xFFF5F5FF),
    surface = Color(0xFFFFFFFF),
)

private val ShotlistTypography = Typography()

@Composable
fun ShotlistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GlassDark
        else -> GlassLight
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShotlistTypography,
        content = content,
    )
}
