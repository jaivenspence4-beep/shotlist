package app.shotlist.ui.glass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    val hazeStyle = HazeMaterials.ultraThin().copy(
        blurRadius = 42.dp,
        noiseFactor = 0.07f,
    )
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = modifier
                .clip(shape)
                .hazeEffect(state = hazeState, style = hazeStyle)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            surface.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            surface.copy(alpha = 0.28f),
                        ),
                    ),
                )
                .border(
                    BorderStroke(
                        1.25.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.82f),
                                Color.White.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                Color.White.copy(alpha = 0.10f),
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

/** Saturated light pools give the blur visible depth without becoming content. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val transition = rememberInfiniteTransition(label = "glass-drift")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb-drift",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.58f
        val primaryCenter = Offset(
            size.width * (0.05f + drift * 0.035f),
            size.height * (0.18f + drift * 0.015f),
        )
        val secondaryCenter = Offset(
            size.width * (0.98f - drift * 0.025f),
            size.height * (0.53f + drift * 0.02f),
        )
        val tertiaryCenter = Offset(
            size.width * (0.22f + drift * 0.025f),
            size.height * (0.92f - drift * 0.018f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.52f), primary.copy(alpha = 0.10f), Color.Transparent),
                center = primaryCenter,
                radius = radius,
            ),
            radius = radius,
            center = primaryCenter,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.37f), Color.Transparent),
                center = secondaryCenter,
                radius = radius * 0.9f,
            ),
            radius = radius * 0.9f,
            center = secondaryCenter,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.33f), Color.Transparent),
                center = tertiaryCenter,
                radius = radius * 0.72f,
            ),
            radius = radius * 0.72f,
            center = tertiaryCenter,
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
            primary.copy(alpha = 0.28f),
            Color(0xFF171B34),
            secondary.copy(alpha = 0.22f),
            background,
        ),
    )
}
