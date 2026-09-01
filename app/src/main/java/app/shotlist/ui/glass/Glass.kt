package app.shotlist.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun GlassPanel(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    val hazeStyle = HazeStyle(
        backgroundColor = surface,
        tint = HazeTint(surface.copy(alpha = 0.34f)),
        blurRadius = 38.dp,
        noiseFactor = 0.08f,
        fallbackTint = HazeTint(surface.copy(alpha = 0.88f)),
    )
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = modifier
                .clip(shape)
                .hazeEffect(state = hazeState, style = hazeStyle)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            surface.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.09f),
                            accent.copy(alpha = 0.11f),
                            surface.copy(alpha = 0.18f),
                        ),
                    ),
                )
                .border(
                    BorderStroke(
                        0.8.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.58f),
                                Color.White.copy(alpha = 0.10f),
                                accent.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.05f),
                            ),
                        ),
                    ),
                    shape,
                )
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun glassBackgroundBrush(): Brush {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val background = MaterialTheme.colorScheme.background
    return Brush.linearGradient(
        colors = listOf(
            background,
            MaterialTheme.colorScheme.surfaceVariant,
            primary.copy(alpha = 0.30f),
            MaterialTheme.colorScheme.surface,
            secondary.copy(alpha = 0.20f),
            background,
        ),
    )
}
