package com.smarteq.pro.audio.engine

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Pure curve arithmetic shared by the engine, the UI and the adaptive tuner.
 * No Android dependencies, so it is unit-tested on the JVM (see EqCurveMathTest).
 */
object EqCurveMath {

    /** Centre frequencies of the engine's internal bands (ISO 1/3-octave-derived, 15 bands). */
    val CENTERS_15: List<Float> = listOf(
        25f, 40f, 63f, 100f, 160f, 250f, 400f, 630f, 1000f, 1600f, 2500f, 4000f, 6300f, 10000f, 16000f,
    )

    /**
     * DynamicsProcessing EQ bands are defined by their TOP edge, and each band covers the
     * range from the previous band's top edge. The edge between two centres is their
     * geometric mean; the last band runs to 20 kHz. Strictly ascending, as the API requires.
     */
    val BAND_TOP_EDGES_15: List<Float> = CENTERS_15.mapIndexed { i, c ->
        if (i == CENTERS_15.lastIndex) 20000f else sqrt(c * CENTERS_15[i + 1])
    }

    const val MAX_TOTAL_GAIN_DB = 12f

    /**
     * Resamples a curve defined at [fromCenters] onto [CENTERS_15], interpolating linearly
     * in log-frequency and holding the end values flat beyond the first/last point.
     */
    fun resampleTo15(gains: List<Float>, fromCenters: List<Float>): List<Float> {
        if (gains.isEmpty()) return List(CENTERS_15.size) { 0f }
        if (gains.size == CENTERS_15.size && fromCenters == CENTERS_15) return gains
        return CENTERS_15.map { f -> interpolate(gains, fromCenters, f) }
    }

    /** Reads the curve's value at any frequency (used by the basic-Equalizer fallback). */
    fun sampleCurveAt(gains15: List<Float>, frequencyHz: Float): Float =
        interpolate(gains15, CENTERS_15, frequencyHz)

    private fun interpolate(gains: List<Float>, centers: List<Float>, f: Float): Float {
        val n = minOf(gains.size, centers.size)
        if (n == 0) return 0f
        if (f <= centers[0]) return gains[0]
        if (f >= centers[n - 1]) return gains[n - 1]
        for (i in 0 until n - 1) {
            val a = centers[i]
            val b = centers[i + 1]
            if (f in a..b) {
                val t = (ln(f) - ln(a)) / (ln(b) - ln(a))
                return gains[i] + (gains[i + 1] - gains[i]) * t
            }
        }
        return gains[n - 1]
    }

    /**
     * Broad bass and treble controls as per-band weights: full effect at and below 100 Hz
     * (half at 160 Hz), full effect at and above 10 kHz (half at 6.3 kHz).
     */
    private val BASS_WEIGHTS = listOf(1f, 1f, 1f, 1f, 0.5f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private val TREBLE_WEIGHTS = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.5f, 1f, 1f)

    /**
     * Sums the layers into the final 15-band curve and clamps it.
     * Order does not matter (they add), but each layer is clamped separately first
     * so a single extreme layer cannot hide the others.
     */
    fun mergeLayers(
        manual15: List<Float>,
        bassDb: Float,
        trebleDb: Float,
        adaptive15: List<Float>,
    ): List<Float> = CENTERS_15.indices.map { i ->
        val sum = manual15.getOrElse(i) { 0f } +
            bassDb * BASS_WEIGHTS[i] + trebleDb * TREBLE_WEIGHTS[i] +
            adaptive15.getOrElse(i) { 0f }
        roundTenth(sum.coerceIn(-MAX_TOTAL_GAIN_DB, MAX_TOTAL_GAIN_DB))
    }

    /**
     * Pre-gain that keeps boosts from driving the limiter all the time: half of the
     * largest boost is taken back off the input. Cuts need no compensation.
     */
    fun headroomDb(final15: List<Float>): Float = -0.5f * max(0f, final15.maxOrNull() ?: 0f)

    /** Moves [from] toward [to] by at most [maxStepDb] per band. Used to ramp changes inaudibly. */
    fun stepToward(from: List<Float>, to: List<Float>, maxStepDb: Float): List<Float> =
        CENTERS_15.indices.map { i ->
            val a = from.getOrElse(i) { 0f }
            val b = to.getOrElse(i) { 0f }
            val d = b - a
            if (abs(d) <= maxStepDb) b else a + maxStepDb * (if (d > 0) 1 else -1)
        }

    fun roundTenth(v: Float): Float = round(v * 10f) / 10f
}
