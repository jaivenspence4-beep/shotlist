package app.shotlist.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

@Composable
fun GlassPanel(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .clip(shape)
            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        surface.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.08f),
                        surface.copy(alpha = 0.22f),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.58f),
                            Color.White.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        ),
                    ),
                ),
                shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun glassBackgroundBrush(): Brush {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val background = MaterialTheme.colorScheme.background
    return Brush.linearGradient(
        colors = listOf(
            background,
            primary.copy(alpha = 0.20f),
            Color(0xFF171827),
            secondary.copy(alpha = 0.18f),
            background,
        ),
    )
}
