package app.shotlist.ui.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CelebrationParticles(
    progress: Float,
    intense: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFFF7BB8),
        Color(0xFFFFD166),
    )
    Canvas(modifier) {
        val count = if (intense) 56 else 36
        val travel = size.minDimension * if (intense) 0.70f else 0.57f
        repeat(count) { index ->
            val angle = index * (Math.PI * 2.0 / count) + (index % 5) * 0.09
            val speed = 0.68f + (index % 7) * 0.055f
            val radius = 34f + progress * travel * speed
            val gravity = progress * progress * (80f + (index % 4) * 28f)
            val center = this.center
            drawCircle(
                color = colors[index % colors.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                radius = 3.5f + (index % 3) * 1.5f,
                center = androidx.compose.ui.geometry.Offset(
                    x = center.x + cos(angle).toFloat() * radius,
                    y = center.y + sin(angle).toFloat() * radius + gravity,
                ),
            )
        }
    }
}

/** Fires only when Inbox transitions from non-empty to empty during this session. */
@Composable
fun InboxZeroCelebration(
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1_350))
        }
    }
    if (progress.value < 1f) {
        val fade = if (progress.value < 0.72f) {
            1f
        } else {
            ((1f - progress.value) / 0.28f).coerceIn(0f, 1f)
        }
        Box(
            modifier = modifier
                .graphicsLayer { alpha = fade }
                .background(Color(0x66060A16)),
        ) {
            CelebrationParticles(
                progress = progress.value,
                intense = true,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val entrance = min(1f, progress.value / 0.18f)
                        scaleX = 0.76f + entrance * 0.24f
                        scaleY = scaleX
                    },
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(74.dp),
                )
                Text(
                    "Inbox zero",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Nothing waiting on you.",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
