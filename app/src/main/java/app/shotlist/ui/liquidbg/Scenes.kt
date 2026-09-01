package app.shotlist.ui.liquidbg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

// Scene models are seeded once (deterministic). Dot/beam PARAMS are
// allocation-free per frame; gradient Brush objects for the moving beams are
// necessarily rebuilt per frame (their geometry moves) — the static
// background gradients are hoisted below.
internal sealed interface Scene {
    data class PhaseBeam(val beams: List<Beam>) : Scene
    data class NoiseField(val dots: List<Dot>) : Scene
    data class Fireflies(val dots: List<Dot>) : Scene
}

internal data class Beam(
    val phase: Float,      // 0..1 start offset along the drift path
    val speed: Float,      // path traversals per ~40s
    val breadth: Float,    // fraction of min dimension
    val length: Float,     // fraction of diagonal
    val tilt: Float,       // degrees
    val alpha: Float,
    val thin: Boolean,     // the bright accent lines of the original
)

internal data class Dot(
    val x: Float, val y: Float,          // home position, 0..1
    val r: Float,                        // radius fraction of min dimension
    val phase: Float, val wobble: Float, // motion personality
    val flicker: Float,                  // fireflies only
)

// Locally seeded per call with a stable per-scene seed: geometry is identical
// every time a scene opens, regardless of what was viewed before (review
// catch — a shared mutable rng made scenes order-dependent).
internal fun seedBeams(seed: Int = 20101122): List<Beam> {
    val rng = Random(seed) // 2010-11-22: the era PhaseBeam shipped
    return List(5) {
        Beam(
            phase = rng.nextFloat(),
            speed = 0.5f + rng.nextFloat() * 0.7f,
            breadth = 0.10f + rng.nextFloat() * 0.16f,
            length = 0.55f + rng.nextFloat() * 0.35f,
            tilt = -32f + rng.nextFloat() * 8f,
            alpha = 0.08f + rng.nextFloat() * 0.08f,
            thin = false,
        )
    } + List(2) {
        Beam(
            phase = rng.nextFloat(),
            speed = 1.1f + rng.nextFloat() * 0.6f,
            breadth = 0.004f,
            length = 0.5f + rng.nextFloat() * 0.3f,
            tilt = -30f,
            alpha = 0.30f,
            thin = true,
        )
    }
}

internal fun seedDots(count: Int, seed: Int): List<Dot> {
    val rng = Random(seed)
    return List(count) {
        Dot(
            x = rng.nextFloat(), y = rng.nextFloat(),
            r = 0.004f + rng.nextFloat() * 0.010f,
            phase = rng.nextFloat() * 6.28f,
            wobble = 0.015f + rng.nextFloat() * 0.03f,
            flicker = 0.6f + rng.nextFloat() * 2.2f,
        )
    }
}

// ---------------------------------------------------------------- PhaseBeam

private val beamDeepTop = Color(0xFF060A1E)
private val beamDeepMid = Color(0xFF10214F)
private val beamDeepBot = Color(0xFF1B3A78)
private val beamLight = Color(0xFF8FC2FF)

// Static backgrounds hoisted — coordinate-free vertical gradients adapt to
// the draw area, so one instance serves every frame.
private val phaseBeamBg =
    Brush.verticalGradient(0f to beamDeepTop, 0.55f to beamDeepMid, 1f to beamDeepBot)

internal fun DrawScope.drawPhaseBeam(scene: Scene.PhaseBeam, t: Float) {
    drawRect(phaseBeamBg)
    val diag = size.width + size.height
    scene.beams.forEach { b ->
        // Drift diagonally up-right, wrapping with margin so entry/exit is soft.
        val progress = ((t * b.speed / 40f) + b.phase) % 1.3f - 0.15f
        val cx = progress * size.width * 1.3f
        val cy = size.height * (1.1f - progress * 1.2f)
        val len = diag * b.length
        val wide = size.minDimension * b.breadth
        rotate(degrees = b.tilt, pivot = Offset(cx, cy)) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to beamLight.copy(alpha = 0f),
                    0.5f to beamLight.copy(alpha = b.alpha),
                    1f to beamLight.copy(alpha = 0f),
                    startX = cx - len / 2, endX = cx + len / 2,
                ),
                topLeft = Offset(cx - len / 2, cy - wide / 2),
                size = Size(len, wide),
                blendMode = BlendMode.Plus,
            )
        }
    }
}

// --------------------------------------------------------------- NoiseField

private val fieldTop = Color(0xFF05070F)
private val fieldBot = Color(0xFF0D1B33)
private val dotBlue = Color(0xFFBFD9FF)

private val fieldBg = Brush.verticalGradient(0f to fieldTop, 1f to fieldBot)

internal fun DrawScope.drawNoiseField(scene: Scene.NoiseField, t: Float, taps: List<TapPulse>) {
    drawRect(fieldBg)
    scene.dots.forEach { d ->
        var px = (d.x + 0.02f * sin(t * 0.11f + d.phase) + t * 0.006f) % 1f
        var py = d.y + d.wobble * sin(t * 0.23f + d.phase * 2.1f)
        var pos = Offset(px * size.width, py * size.height)
        pos = nudgeTowardTaps(pos, taps, t, strength = 0.25f)
        val r = d.r * size.minDimension
        // soft glow, then core
        drawCircle(dotBlue.copy(alpha = 0.10f), radius = r * 3.2f, center = pos)
        drawCircle(dotBlue.copy(alpha = 0.55f), radius = r, center = pos)
    }
}

// ---------------------------------------------------------------- Fireflies

private val nightTop = Color(0xFF070B08)
private val nightBot = Color(0xFF12200F)
private val ember = Color(0xFFFFD98C)

private val nightBg = Brush.verticalGradient(0f to nightTop, 1f to nightBot)

internal fun DrawScope.drawFireflies(scene: Scene.Fireflies, t: Float, taps: List<TapPulse>) {
    drawRect(nightBg)
    scene.dots.forEach { d ->
        val px = d.x + 0.05f * sin(t * 0.17f + d.phase) + 0.02f * cos(t * 0.31f + d.phase * 3f)
        val py = d.y + 0.04f * cos(t * 0.13f + d.phase * 1.7f)
        var pos = Offset((px.mod(1f)) * size.width, (py.mod(1f)) * size.height)
        pos = nudgeTowardTaps(pos, taps, t, strength = 0.45f)
        val glow = 0.35f + 0.35f * (0.5f + 0.5f * sin(t * d.flicker + d.phase))
        val r = d.r * size.minDimension * 1.2f
        drawCircle(ember.copy(alpha = glow * 0.16f), radius = r * 4f, center = pos)
        drawCircle(ember.copy(alpha = glow), radius = r, center = pos)
    }
}

/** Gentle attraction toward taps younger than ~2.5s, easing out. */
private fun DrawScope.nudgeTowardTaps(
    pos: Offset,
    taps: List<TapPulse>,
    t: Float,
    strength: Float,
): Offset {
    var out = pos
    taps.forEach { tap ->
        val age = t - tap.tAtBirth
        if (age in 0f..2.5f) {
            val d = tap.at - out
            val dist = abs(d.x) + abs(d.y)
            if (dist < size.minDimension * 0.9f) {
                val pull = strength * exp(-age * 1.6f) * exp(-dist / (size.minDimension * 0.5f))
                out += d * pull
            }
        }
    }
    return out
}
