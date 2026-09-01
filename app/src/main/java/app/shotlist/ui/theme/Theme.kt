package app.shotlist.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
    primary = Color(0xFF7DA8FF),
    onPrimary = Color(0xFF06122B),
    secondary = Color(0xFF9BE8D8),
    background = Color(0xFF0B0D12),
    surface = Color(0xFF12151D),
    onBackground = Color(0xFFE6EAF2),
    onSurface = Color(0xFFE6EAF2),
)

private val GlassLight = lightColorScheme(
    primary = Color(0xFF2F5FD0),
    secondary = Color(0xFF1E8C77),
    background = Color(0xFFF4F6FB),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun ShotlistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> GlassDark
        else -> GlassLight
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
