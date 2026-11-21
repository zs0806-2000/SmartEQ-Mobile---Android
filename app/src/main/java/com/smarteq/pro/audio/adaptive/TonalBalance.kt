package com.smarteq.pro.audio.adaptive

import com.smarteq.pro.audio.engine.EqCurveMath
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/**
 * The maths behind adaptive tuning. Pure Kotlin (no Android) so it is unit-tested on the JVM.
 *
 * Idea, in plain terms: measure each track's long-term tonal balance (how much energy sits in
 * each of the 15 bands), compare it with the average balance of the music you play on the
 * same output device, and gently EQ the track toward that average. A thin, bass-light master
 * gets some low end back; a dull one gets some air; a track that already matches is left alone.
 *
 * This follows published findings that commercial recordings cluster around a common long-term
 * spectral contour (Pestana et al., 2013, AES Convention 135) and that such averages can serve as
 * automatic-EQ targets (Elowsson & Friberg, 2017). The target here is learned from the user's own
 * listening rather than taken from a fixed published curve.
 */
object TonalBalance {

    val BAND_COUNT = EqCurveMath.CENTERS_15.size

    /** Lower edge of each band: 20 Hz for the first, then the previous band's top edge. */
    private val LOW_EDGES: List<Float> = List(BAND_COUNT) { i ->
        if (i == 0) 20f else EqCurveMath.BAND_TOP_EDGES_15[i - 1]
    }
    private val OCTAVE_WIDTHS: List<Float> = List(BAND_COUNT) { i ->
        log2(EqCurveMath.BAND_TOP_EDGES_15[i] / LOW_EDGES[i])
    }

    /** Bands used as the level reference when normalising a shape (160 Hz to 4 kHz). */
    private val REFERENCE_BANDS = 4..11

    /** Largest per-band correction adaptive tuning may apply. Kept small on purpose. */
    const val MAX_CORRECTION_DB = 4f

    const val MIN_BINS_PER_BAND = 2

    /**
     * Adds the energy of one Visualizer FFT capture to [accumulator] (linear power per band).
     * Visualizer FFT layout: [0] = DC real, [1] = Nyquist real, then (re, im) pairs for bins
     * 1 .. n/2-1. Returns false if the frame is near-silent and was skipped.
     *
     * @param samplingRateHz the Visualizer's sampling rate in Hz (it reports milliHertz).
     */
    fun accumulateFft(fft: ByteArray, samplingRateHz: Float, accumulator: DoubleArray, binsPerBand: IntArray): Boolean {
        val n = fft.size
        if (n < 4 || samplingRateHz <= 0f) return false
        val binHz = samplingRateHz / n
        val frame = DoubleArray(BAND_COUNT)
        var total = 0.0
        var k = 1
        while (2 * k + 1 < n) {
            val re = fft[2 * k].toDouble()
            val im = fft[2 * k + 1].toDouble()
            val p = re * re + im * im
            val f = k * binHz
            val band = bandOf(f)
            if (band >= 0) {
                frame[band] += p
                total += p
            }
            k++
        }
        // 8-bit captures of silence are mostly 0/±1 values; require clearly more than that.
        if (total < n * 4.0) return false
        for (b in 0 until BAND_COUNT) accumulator[b] += frame[b]
        if (binsPerBand.all { it == 0 }) {
            var kk = 1
            while (2 * kk + 1 < n) {
                val band = bandOf(kk * binHz)
                if (band >= 0) binsPerBand[band]++
                kk++
            }
        }
        return true
    }

    private fun bandOf(f: Float): Int {
        if (f < LOW_EDGES[0] || f >= EqCurveMath.BAND_TOP_EDGES_15.last()) return -1
        for (i in 0 until BAND_COUNT) if (f < EqCurveMath.BAND_TOP_EDGES_15[i]) return i
        return -1
    }

    /**
     * Turns accumulated band power into a shape in dB per octave, normalised so the
     * 160 Hz–4 kHz reference region averages 0 dB. Bands covered by fewer than
     * [MIN_BINS_PER_BAND] FFT bins are returned as NaN ("not measured"): one bin is too coarse
     * to estimate a band's level. With Android's usual 1024-point capture (~47 Hz per bin) this
     * means the bands below ~125 Hz follow the 160 Hz band instead of being measured.
     */
    fun toShapeDb(accumulator: DoubleArray, binsPerBand: IntArray): FloatArray {
        val raw = FloatArray(BAND_COUNT) { b ->
            if (binsPerBand[b] < MIN_BINS_PER_BAND || accumulator[b] <= 0.0) Float.NaN
            else (10.0 * log10(accumulator[b] / OCTAVE_WIDTHS[b])).toFloat()
        }
        val ref = REFERENCE_BANDS.map { raw[it] }.filter { !it.isNaN() }
        val mean = if (ref.isEmpty()) 0f else ref.average().toFloat()
        return FloatArray(BAND_COUNT) { raw[it] - mean }
    }

    /**
     * Removes the part of a measurement caused by our own EQ, if the analyser hears the audio
     * after the effect. [postEffectWeight] is 0 (analyser hears the raw track), 1 (it hears our
     * output) or in between while that is still being learned.
     */
    fun removeOwnEq(measured: FloatArray, applied15: List<Float>, postEffectWeight: Float): FloatArray {
        val w = postEffectWeight.coerceIn(0f, 1f)
        val out = FloatArray(BAND_COUNT) { b ->
            if (measured[b].isNaN()) Float.NaN else measured[b] - w * applied15.getOrElse(b) { 0f }
        }
        // Re-normalise to the same reference region.
        val ref = REFERENCE_BANDS.map { out[it] }.filter { !it.isNaN() }
        val mean = if (ref.isEmpty()) 0f else ref.average().toFloat()
        return FloatArray(BAND_COUNT) { out[it] - mean }
    }

    /**
     * Folds one track's shape into the learned target (running average over up to
     * [maxTracks] tracks, so the target keeps adapting to what you listen to).
     */
    fun learn(target: FloatArray?, tracksLearned: Int, shape: FloatArray, maxTracks: Int = 40): FloatArray {
        if (target == null || tracksLearned <= 0) return shape.copyOf()
        val w = 1f / min(tracksLearned + 1, maxTracks)
        return FloatArray(BAND_COUNT) { b ->
            when {
                shape[b].isNaN() -> target[b]
                target[b].isNaN() -> shape[b]
                else -> target[b] + w * (shape[b] - target[b])
            }
        }
    }

    /**
     * How much of the computed correction to trust, from 0 to 1. With only a few tracks
     * learned the target is still mostly "the last few songs", so corrections stay small.
     */
    fun confidence(tracksLearned: Int): Float = ((tracksLearned - 1).coerceAtLeast(0) / 8f).coerceAtMost(1f)

    /**
     * Correction that moves [shape] toward [target], scaled by [strength] (0..1) and confidence,
     * smoothed across neighbouring bands (so it never makes narrow peaks), and limited to
     * ±[MAX_CORRECTION_DB]. Unmeasured bands borrow from the nearest measured band.
     */
    fun correction(shape: FloatArray, target: FloatArray, strength: Float, tracksLearned: Int): List<Float> {
        val scale = strength.coerceIn(0f, 1f) * confidence(tracksLearned)
        val diff = FloatArray(BAND_COUNT) { b ->
            if (shape[b].isNaN() || target[b].isNaN()) Float.NaN else target[b] - shape[b]
        }
        fillGaps(diff)
        val smoothed = FloatArray(BAND_COUNT) { b ->
            val l = diff[max(0, b - 1)]
            val r = diff[min(BAND_COUNT - 1, b + 1)]
            0.25f * l + 0.5f * diff[b] + 0.25f * r
        }
        // Shapes are only known relative to each other, so a constant offset carries no meaning.
        // Removing the median keeps the correction about balance, not overall volume, and keeps
        // the bands that already match (usually most of them) at 0 dB.
        val median = smoothed.sorted().let { (it[it.size / 2] + it[(it.size - 1) / 2]) / 2f }
        return smoothed.map { v ->
            EqCurveMath.roundTenth(((v - median) * scale).coerceIn(-MAX_CORRECTION_DB, MAX_CORRECTION_DB))
        }
    }

    /** Replaces NaN entries with the nearest measured value, or 0 if nothing was measured. */
    fun fillGaps(values: FloatArray) {
        if (values.all { it.isNaN() }) {
            values.fill(0f); return
        }
        for (b in values.indices) {
            if (!values[b].isNaN()) continue
            var d = 1
            var found = Float.NaN
            while (found.isNaN() && d < values.size) {
                val l = b - d
                val r = b + d
                if (l >= 0 && !values[l].isNaN()) found = values[l]
                else if (r < values.size && !values[r].isNaN()) found = values[r]
                d++
            }
            values[b] = found
        }
    }

    /**
     * Estimates whether the analyser hears our processed output (returns ~1) or the raw track
     * (returns ~0), from how the measured shape moved after we changed the EQ by [appliedDelta].
     * Returns null if the change was too small to tell.
     */
    fun estimatePostEffect(before: FloatArray, after: FloatArray, appliedDelta: List<Float>): Float? {
        var num = 0.0
        var den = 0.0
        val dMean = REFERENCE_BANDS.map { appliedDelta.getOrElse(it) { 0f } }.average().toFloat()
        for (b in 0 until BAND_COUNT) {
            if (before[b].isNaN() || after[b].isNaN()) continue
            val d = appliedDelta.getOrElse(b) { 0f } - dMean
            num += (after[b] - before[b]) * d
            den += d * d
        }
        if (den < 1.5 * 1.5 * 4) return null // need at least ~1.5 dB RMS over a few bands
        return (num / den).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Loudness match: gain that brings this track's level toward the learned average level,
     * limited to -6 dB … +3 dB (boosting is kept small to protect headroom).
     */
    fun loudnessGain(trackRmsDb: Float, targetRmsDb: Float, strength: Float): Float =
        EqCurveMath.roundTenth(((targetRmsDb - trackRmsDb) * strength.coerceIn(0f, 1f)).coerceIn(-6f, 3f))

    fun maxAbs(values: List<Float>): Float = values.maxOfOrNull { abs(it) } ?: 0f
}
