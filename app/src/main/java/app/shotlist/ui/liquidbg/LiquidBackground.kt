package app.shotlist.ui.liquidbg

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.withFrameNanos

/**
 * Living backgrounds — the early-Android live-wallpaper feeling, reborn under
 * the glass. "phasebeam" and "noisefield" are faithful ports of the AOSP
 * classics (Apache-2.0, see NOTICE); "fireflies" is ours, and its swarm grows
 * with the user's streak.
 *
 * Battery discipline: the frame clock only ticks while this composable is in
 * an active composition and being drawn (withFrameNanos rides Choreographer,
 * which pauses off-screen), and animation is disabled entirely in power-save
 * mode — the scene renders one rich still frame instead.
 */
@Composable
fun LiquidBackground(
    sceneKey: String,
    modifier: Modifier = Modifier,
    streak: Int = 0,
) {
    val context = LocalContext.current
    val animate = remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isPowerSaveMode != true
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate, sceneKey) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) t += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }

    // Recent taps: scenes react gently (dots drift toward, fireflies swarm).
    var taps by remember { mutableStateOf(listOf<TapPulse>()) }

    val scene = remember(sceneKey, streak) { buildScene(sceneKey, streak) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(sceneKey) {
                detectTapGestures { offset ->
                    taps = (taps + TapPulse(offset, tAtBirth = t)).takeLast(3)
                }
            },
    ) {
        when (scene) {
            is Scene.PhaseBeam -> drawPhaseBeam(scene, t)
            is Scene.NoiseField -> drawNoiseField(scene, t, taps)
            is Scene.Fireflies -> drawFireflies(scene, t, taps)
        }
    }
}

internal data class TapPulse(val at: Offset, val tAtBirth: Float)

/** Keys the theme picker persists. Anything unknown falls back to PhaseBeam. */
fun liquidSceneKeys(): List<String> = listOf("phasebeam", "noisefield", "fireflies")

internal fun buildScene(key: String, streak: Int): Scene = when (key) {
    "noisefield" -> Scene.NoiseField(seedDots(count = 42))
    "fireflies" -> Scene.Fireflies(seedDots(count = 14 + streak.coerceIn(0, 30)))
    else -> Scene.PhaseBeam(seedBeams())
}
