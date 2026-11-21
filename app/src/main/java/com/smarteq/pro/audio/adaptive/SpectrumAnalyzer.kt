package com.smarteq.pro.audio.adaptive

import android.media.audiofx.Visualizer
import android.util.Log

/** One analysis window's result. */
data class SpectrumMeasurement(
    /** Tonal balance in dB per octave, 15 bands, NaN where not measured. */
    val shapeDb: FloatArray,
    /** Average RMS level in dBFS, or null if the device does not report it. */
    val rmsDb: Float?,
    val frames: Int,
)

/**
 * Listens to the audio of one session through Android's Visualizer effect and accumulates its
 * long-term spectrum. The Visualizer only provides a low-resolution 8-bit snapshot meant for
 * visualisation; it cannot record audio, and nothing it returns is stored or sent anywhere.
 * Only the 15 averaged band levels survive, in memory.
 *
 * Android requires the RECORD_AUDIO ("Microphone") permission to use it. The microphone itself
 * is never opened.
 */
class SpectrumAnalyzer {

    private var visualizer: Visualizer? = null
    private var attachedSession: Int = -1
    private var samplingRateHz = 0f

    private val lock = Any()
    private var power = DoubleArray(TonalBalance.BAND_COUNT)
    private val binsPerBand = IntArray(TonalBalance.BAND_COUNT)
    private var frames = 0
    private var rmsSumDb = 0.0
    private var rmsCount = 0

    val currentSession: Int get() = attachedSession

    /** Starts (or moves) the analyser to [sessionId]. Returns false if Android refused. */
    fun start(sessionId: Int): Boolean {
        if (visualizer != null && attachedSession == sessionId) return true
        stop()
        return try {
            val v = Visualizer(sessionId)
            v.setEnabled(false)
            val range = Visualizer.getCaptureSizeRange()
            v.setCaptureSize(range[1].coerceAtMost(1024))
            runCatching { v.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS) }
            runCatching { v.setScalingMode(Visualizer.SCALING_MODE_AS_PLAYED) }
            samplingRateHz = v.samplingRate / 1000f
            binsPerBand.fill(0)
            val rateMilliHz = Visualizer.getMaxCaptureRate().coerceAtMost(10_000)
            val peakRms = Visualizer.MeasurementPeakRms()
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null) return
                    synchronized(lock) {
                        val rate = if (samplingRate > 0) samplingRate / 1000f else samplingRateHz
                        if (TonalBalance.accumulateFft(fft, rate, power, binsPerBand)) {
                            frames++
                            val rms = runCatching {
                                vis?.getMeasurementPeakRms(peakRms)
                                peakRms.mRms / 100f // millibels -> dB
                            }.getOrNull()
                            if (rms != null && rms > -90f) {
                                rmsSumDb += rms; rmsCount++
                            }
                        }
                    }
                }
            }, rateMilliHz, false, true)
            v.setEnabled(true)
            visualizer = v
            attachedSession = sessionId
            reset()
            true
        } catch (e: Exception) {
            // Thrown when the permission is missing, the session is gone, or the device refuses.
            Log.w(TAG, "Visualizer unavailable for session $sessionId", e)
            visualizer = null
            attachedSession = -1
            false
        }
    }

    fun stop() {
        runCatching {
            visualizer?.setEnabled(false)
            visualizer?.release()
        }
        visualizer = null
        attachedSession = -1
    }

    fun reset() = synchronized(lock) {
        power = DoubleArray(TonalBalance.BAND_COUNT)
        frames = 0
        rmsSumDb = 0.0
        rmsCount = 0
    }

    fun framesSoFar(): Int = synchronized(lock) { frames }

    /** Returns what was gathered since the last reset, then resets. Null if too little audio. */
    fun takeMeasurement(minFrames: Int): SpectrumMeasurement? = synchronized(lock) {
        val result = if (frames < minFrames) null else SpectrumMeasurement(
            shapeDb = TonalBalance.toShapeDb(power, binsPerBand),
            rmsDb = if (rmsCount > 0) (rmsSumDb / rmsCount).toFloat() else null,
            frames = frames,
        )
        power = DoubleArray(TonalBalance.BAND_COUNT)
        frames = 0
        rmsSumDb = 0.0
        rmsCount = 0
        result
    }

    private companion object {
        const val TAG = "SpectrumAnalyzer"
    }
}
