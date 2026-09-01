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
import app.shotlist.ui.theme.LocalOrbStyle
import app.shotlist.ui.theme.OrbStyle
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

/** Saturated light pools give the blur visible depth without becoming content. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val orbStyle = LocalOrbStyle.current
    val transition = rememberInfiniteTransition(label = "glass-drift")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (orbStyle) {
                    OrbStyle.DRIFT -> 14_000
                    OrbStyle.HALO -> 20_000
                    OrbStyle.AURORA -> 9_000
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb-drift",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = size.minDimension * when (orbStyle) {
            OrbStyle.DRIFT -> 0.58f
            OrbStyle.HALO -> 0.48f
            OrbStyle.AURORA -> 0.70f
        }
        val primaryCenter = when (orbStyle) {
            OrbStyle.DRIFT -> Offset(size.width * (0.02f + drift * 0.04f), size.height * (0.16f + drift * 0.018f))
            OrbStyle.HALO -> Offset(size.width * 0.50f, size.height * (0.24f + drift * 0.012f))
            OrbStyle.AURORA -> Offset(size.width * (0.18f + drift * 0.16f), size.height * 0.10f)
        }
        val secondaryCenter = when (orbStyle) {
            OrbStyle.DRIFT -> Offset(size.width * (1.02f - drift * 0.03f), size.height * (0.48f + drift * 0.025f))
            OrbStyle.HALO -> Offset(size.width * 0.50f, size.height * 0.52f)
            OrbStyle.AURORA -> Offset(size.width * (0.82f - drift * 0.13f), size.height * 0.50f)
        }
        val tertiaryCenter = when (orbStyle) {
            OrbStyle.DRIFT -> Offset(size.width * (0.18f + drift * 0.03f), size.height * (0.88f - drift * 0.022f))
            OrbStyle.HALO -> Offset(size.width * 0.50f, size.height * 0.80f)
            OrbStyle.AURORA -> Offset(size.width * (0.25f - drift * 0.10f), size.height * 0.91f)
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.62f), tertiary.copy(alpha = 0.13f), Color.Transparent),
                center = primaryCenter,
                radius = radius,
            ),
            radius = radius,
            center = primaryCenter,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.45f), primary.copy(alpha = 0.08f), Color.Transparent),
                center = secondaryCenter,
                radius = radius * 0.9f,
            ),
            radius = radius * 0.9f,
            center = secondaryCenter,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.40f), primary.copy(alpha = 0.10f), Color.Transparent),
                center = tertiaryCenter,
                radius = radius * 0.72f,
            ),
            radius = radius * 0.72f,
            center = tertiaryCenter,
        )
        drawCircle(
            color = secondary.copy(alpha = if (orbStyle == OrbStyle.HALO) 0.20f else 0.08f),
            radius = radius * 0.48f,
            center = if (orbStyle == OrbStyle.HALO) primaryCenter else Offset(size.width * 0.88f, size.height * 0.08f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
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
