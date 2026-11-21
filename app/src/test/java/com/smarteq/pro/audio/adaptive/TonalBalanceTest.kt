package com.smarteq.pro.audio.adaptive

import com.smarteq.pro.audio.engine.EqCurveMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class TonalBalanceTest {

    /** Builds a Visualizer-style 8-bit FFT from a power-per-Hz function. */
    private fun fakeFft(n: Int, fs: Float, density: (Float) -> Double): ByteArray {
        val out = ByteArray(n)
        val binHz = fs / n
        var k = 1
        while (2 * k + 1 < n) {
            out[2 * k] = sqrt(density(k * binHz)).roundToInt().coerceIn(-128, 127).toByte()
            k++
        }
        return out
    }

    private fun shapeOf(density: (Float) -> Double): FloatArray {
        val acc = DoubleArray(15)
        val bins = IntArray(15)
        repeat(50) { TonalBalance.accumulateFft(fakeFft(1024, 48000f, density), 48000f, acc, bins) }
        return TonalBalance.toShapeDb(acc, bins)
    }

    private val pink: (Float) -> Double = { f -> 100_000.0 / f }

    @Test fun bandEdgesAscendAndEndAt20k() {
        val e = EqCurveMath.BAND_TOP_EDGES_15
        assertTrue(e.zipWithNext().all { it.first < it.second })
        assertEquals(20000f, e.last())
    }

    @Test fun resampleKeepsFlatCurveFlat() {
        val r = EqCurveMath.resampleTo15(List(5) { 3f }, listOf(63f, 250f, 1000f, 4000f, 16000f))
        assertTrue(r.all { abs(it - 3f) < 1e-4 })
    }

    @Test fun pinkNoiseMeasuresFlatPerOctave() {
        val measured = shapeOf(pink).filter { !it.isNaN() }
        assertTrue(measured.max() - measured.min() < 3f)
    }

    @Test fun bassHeavyTrackGetsBassCutAndMidsUntouched() {
        val target = TonalBalance.learn(null, 0, shapeOf(pink))
        val bassy = shapeOf { f -> pink(f) * (if (f < 300f) 4.0 else 1.0) }
        val c = TonalBalance.correction(bassy, target, 1f, 10)
        assertTrue(c[4] < -1f)
        assertTrue(abs(c[8]) < 0.5f)
        assertTrue(c.all { abs(it) <= TonalBalance.MAX_CORRECTION_DB })
    }

    @Test fun ownEqIsRemovedAndAnalyserPositionDetected() {
        val raw = shapeOf(pink)
        val applied = List(15) { i -> if (i in 3..7) 3f else 0f }
        val heard = FloatArray(15) { if (raw[it].isNaN()) Float.NaN else raw[it] + applied[it] }
        val back = TonalBalance.removeOwnEq(heard, applied, 1f)
        assertTrue((0 until 15).filter { !raw[it].isNaN() }.all { abs(back[it] - raw[it]) < 0.6f })
        assertTrue((TonalBalance.estimatePostEffect(raw, TonalBalance.removeOwnEq(heard, applied, 0f), applied) ?: 0f) > 0.8f)
        assertTrue((TonalBalance.estimatePostEffect(raw, raw, applied) ?: 1f) < 0.2f)
    }

    @Test fun correctionsMakeTracksMoreConsistent() {
        val rnd = java.util.Random(7)
        var target: FloatArray? = null
        var n = 0
        var raw = 0.0
        var fixed = 0.0
        repeat(30) {
            val tilt = rnd.nextGaussian() * 2.0
            val s = shapeOf { f -> pink(f) * 10.0.pow(tilt * log2(f / 1000.0) / 10.0) }
            val t = target
            if (t != null && n >= 9) {
                val c = TonalBalance.correction(s, t, 1f, n)
                for (b in 4..13) if (!s[b].isNaN()) {
                    raw += abs(s[b] - t[b]); fixed += abs(s[b] + c[b] - t[b])
                }
            }
            target = TonalBalance.learn(target, n, s); n++
        }
        assertTrue(fixed < raw * 0.7)
    }

    @Test fun silenceIsIgnored() {
        assertTrue(!TonalBalance.accumulateFft(ByteArray(1024), 48000f, DoubleArray(15), IntArray(15)))
    }
}
