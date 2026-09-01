/*
 * Contains work derived from the Android Open Source Project live wallpapers
 * (platform/packages/wallpapers/ — PhaseBeam, NoiseField),
 * Copyright (C) The Android Open Source Project, Apache License 2.0
 * (see /LICENSES/Apache-2.0.txt). MODIFIED: RenderScript renderers
 * re-implemented as Jetpack Compose Canvas scenes for Shotlist; the
 * Fireflies scene is original Shotlist work.
 */
package app.shotlist.ui.liquidbg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    data class PhaseBeam(val dots: List<Mote>, val beams: List<Mote>) : Scene
    data class NoiseField(val dots: List<Dot>) : Scene
    data class Fireflies(val dots: List<Dot>) : Scene
}

/**
 * A PhaseBeam particle, faithful to the AOSP phasebeam.rs model: position in
 * the original's coordinate space (x drifts, y rises, z gives parallax —
 * speed and size both scale with z exactly as the original computed them).
 */
internal data class Mote(val x0: Float, val y0: Float, val z: Float)

internal data class Dot(
    val x: Float, val y: Float,          // home position, 0..1
    val r: Float,                        // radius fraction of min dimension
    val phase: Float, val wobble: Float, // motion personality
    val flicker: Float,                  // fireflies only
)

// Locally seeded per call with a stable per-scene seed: geometry is identical
// every time a scene opens, regardless of what was viewed before (review
// catch — a shared mutable rng made scenes order-dependent).
/**
 * Seeds per the original phasebeam.rs: dots at x∈[0,3), y∈[-1.25,1.25), on
 * three depth layers (z=25 far, z=14 mid, z∈[6,14) near); beams at
 * x∈[-1.25,1.25), y∈[-1.05,1.205), z∈[2,17.5).
 */
internal fun seedPhaseBeam(seed: Int = 20101122): Scene.PhaseBeam {
    val rng = Random(seed) // 2010-11-22: the era PhaseBeam shipped
    val dots = List(60) { i ->
        val z = when {
            i % 3 == 0 -> 25f
            i % 3 == 1 -> 14f
            else -> 6f + rng.nextFloat() * 8f
        }
        Mote(rng.nextFloat() * 3f, -1.25f + rng.nextFloat() * 2.5f, z)
    }
    val beams = List(12) { i ->
        val z = if (i < 4) (4f + rng.nextFloat() * 6f) / 2f
        else (4f + rng.nextFloat() * 31f) / 2f
        Mote(-1.25f + rng.nextFloat() * 2.5f, -1.05f + rng.nextFloat() * 2.255f, z)
    }
    return Scene.PhaseBeam(dots, beams)
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


// The genuine article: black base fading to deep blue at the horizon, cyan
// dots rising on three parallax layers, vertical light beams climbing with
// them — motion model transcribed from phasebeam.rs (66ms frame → ×15.15
// converts its per-frame speeds to per-second).
private val phaseBg = Brush.verticalGradient(
    0f to Color(0xFF000000), 0.6f to Color(0xFF02050E), 1f to Color(0xFF0A1E3C),
)
private val phaseDot = Color(0xFFAFE3FF)
private val phaseBeamLight = Color(0xFF4FC3F7)
private const val FRAME = 15.15f

internal fun DrawScope.drawPhaseBeam(scene: Scene.PhaseBeam, t: Float) {
    drawRect(phaseBg)
    scene.dots.forEach { m ->
        val y = wrap(m.y0 + 0.00022f * m.z * FRAME * t, -1.25f, 1.25f)
        val x = wrap(m.x0 + 0.0001f * m.z * FRAME * t, 0f, 3f)
        val pos = Offset(
            (x / 3f) * size.width,
            (1f - (y + 1.25f) / 2.5f) * size.height,
        )
        val r = size.minDimension * (0.09f / m.z)
        val a = (0.2f + 2.5f / m.z).coerceAtMost(0.8f)
        drawCircle(phaseDot.copy(alpha = a * 0.15f), r * 3f, pos)
        drawCircle(phaseDot.copy(alpha = a), r, pos)
    }
    scene.beams.forEach { m ->
        val y = wrap(m.y0 + 0.00016f * m.z * FRAME * t, -1.05f, 1.205f)
        val x = wrap(m.x0 + 0.000156f * m.z * FRAME * t, -1.25f, 1.25f)
        val sx = ((x + 1.25f) / 2.5f) * size.width
        val sy = (1f - (y + 1.05f) / 2.255f) * size.height
        val h = size.height * (1.4f / m.z).coerceIn(0.08f, 0.7f)
        val w = (size.minDimension * 0.10f / m.z).coerceAtLeast(3f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to phaseBeamLight.copy(alpha = 0f),
                0.5f to phaseBeamLight.copy(alpha = (1.8f / m.z).coerceAtMost(0.45f)),
                1f to phaseBeamLight.copy(alpha = 0f),
                startY = sy - h / 2, endY = sy + h / 2,
            ),
            topLeft = Offset(sx - w / 2, sy - h / 2),
            size = Size(w, h),
            blendMode = BlendMode.Plus,
        )
    }
}

private fun wrap(v: Float, lo: Float, hi: Float): Float {
    val range = hi - lo
    var r = (v - lo) % range
    if (r < 0) r += range
    return lo + r
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
