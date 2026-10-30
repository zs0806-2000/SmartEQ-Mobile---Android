package com.smarteq.pro.audio.engine

import android.media.audiofx.DynamicsProcessing
import android.util.Log
import com.smarteq.pro.data.model.BandFrequencies
import com.smarteq.pro.data.model.DynamicsSettings

/**
 * Wraps android.media.audiofx.DynamicsProcessing (API 28+) and hardware Equalizer fallback.
 * Supports low-latency Bluetooth processing (VARIANT_FAVOR_SPEED = 1) to minimize
 * filter buffer latency and audio-video sync delays.
 */
class SmartEqDspEngine(
    private val audioSessionId: Int,
) {
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var fallbackEqualizer: android.media.audiofx.Equalizer? = null
    private var fallbackBassBoost: android.media.audiofx.BassBoost? = null
    private var channelCount: Int = 2
    private var currentBandCount: Int = 15
    private var currentLowLatency: Boolean = false

    private companion object {
        const val TAG = "SmartEqDspEngine"
        const val VARIANT_FAVOR_FREQUENCY_RESOLUTION = 0
        const val VARIANT_FAVOR_SPEED = 1
        const val BASS_APPROX_FREQ_HZ = 55f
        const val TREBLE_APPROX_FREQ_HZ = 13000f
        const val LIMITER_THRESHOLD_DB = -0.5f
        const val LIMITER_RATIO = 20f
        const val LIMITER_ATTACK_SEC = 0.001f
        const val LIMITER_RELEASE_SEC = 0.05f
        const val LOW_LATENCY_LIMITER_ATTACK_SEC = 0.0005f
        const val LOW_LATENCY_LIMITER_RELEASE_SEC = 0.015f
    }

    /** True once the engine has successfully attached to this session. */
    var isAttached: Boolean = false
        private set

    fun attach(bandCount: Int, channels: Int = 2, lowLatency: Boolean = false): Boolean {
        try {
            channelCount = channels.coerceAtLeast(1)
            currentBandCount = bandCount
            currentLowLatency = lowLatency
            val totalPostEqBands = bandCount + 2 // + bass-approx + treble-approx

            // VARIANT_FAVOR_SPEED minimizes filter buffer latency and processing delays for Bluetooth
            val variant = if (lowLatency) VARIANT_FAVOR_SPEED else VARIANT_FAVOR_FREQUENCY_RESOLUTION

            val config = DynamicsProcessing.Config.Builder(
                variant,
                channelCount,
                /* preEqInUse = */ false, /* preEqBandCount = */ 0,
                /* mbcInUse = */ true, /* mbcBandCount = */ 1,
                /* postEqInUse = */ true, /* postEqBandCount = */ totalPostEqBands,
                /* limiterInUse = */ true,
            ).build()

            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = true
            }
            isAttached = true
            Log.i(TAG, "Attached DynamicsProcessing to session $audioSessionId (bands: $totalPostEqBands, lowLatency: $lowLatency)")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "DynamicsProcessing unavailable for session $audioSessionId; attempting Equalizer fallback", e)
        }

        // Resilient Fallback: Standard Android audiofx Equalizer + BassBoost
        return try {
            fallbackEqualizer = android.media.audiofx.Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            fallbackBassBoost = android.media.audiofx.BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            isAttached = true
            Log.i(TAG, "Attached fallback hardware Equalizer to session $audioSessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach both DynamicsProcessing and Equalizer fallback to session $audioSessionId", e)
            isAttached = false
            false
        }
    }

    fun release() {
        runCatching {
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
        }.onFailure { Log.w(TAG, "Error releasing DynamicsProcessing for session $audioSessionId", it) }
        dynamicsProcessing = null

        runCatching {
            fallbackEqualizer?.enabled = false
            fallbackEqualizer?.release()
            fallbackBassBoost?.enabled = false
            fallbackBassBoost?.release()
        }.onFailure { Log.w(TAG, "Error releasing fallback effects for session $audioSessionId", it) }
        fallbackEqualizer = null
        fallbackBassBoost = null
        isAttached = false
    }

    /** Applies a full EQ + dynamics configuration in one atomic pass. */
    fun apply(bandGainsDb: List<Float>, dynamics: DynamicsSettings, bandCount: Int, lowLatency: Boolean = currentLowLatency) {
        val dp = dynamicsProcessing
        if (dp != null) {
            if (bandCount != currentBandCount || lowLatency != currentLowLatency) {
                release()
                if (!attach(bandCount, channelCount, lowLatency)) return
            }
            val dp2 = dynamicsProcessing ?: return
            val frequencies = BandFrequencies.forCount(com.smarteq.pro.data.model.BandCount.fromCount(bandCount))

            for (channel in 0 until channelCount) {
                applyMbc(dp2, channel, dynamics, lowLatency)
                applyLimiter(dp2, channel, lowLatency)

                // Band 0: bass-shelf approximation
                setPostEqBand(dp2, channel, 0, BASS_APPROX_FREQ_HZ, dynamics.bassDb)

                // Bands 1..bandCount: the graphic EQ, offset by one for the bass band
                for (i in bandGainsDb.indices) {
                    val freq = frequencies.getOrElse(i) { frequencies.last() }.toFloat()
                    setPostEqBand(dp2, channel, i + 1, freq, bandGainsDb[i])
                }

                // Last band: treble-shelf approximation
                setPostEqBand(dp2, channel, bandCount + 1, TREBLE_APPROX_FREQ_HZ, dynamics.trebleDb)
            }
            return
        }

        val eq = fallbackEqualizer ?: return
        runCatching {
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            for (i in 0 until numBands) {
                val gainDb = bandGainsDb.getOrElse(i) { 0f }
                val millibels = (gainDb * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                eq.setBandLevel(i.toShort(), millibels)
            }
            val bassStrength = ((dynamics.bassDb.coerceIn(0f, 12f) / 12f) * 1000).toInt().toShort()
            fallbackBassBoost?.setStrength(bassStrength)
        }.onFailure { Log.w(TAG, "Fallback Equalizer configuration failed", it) }
    }

    private fun applyMbc(dp: DynamicsProcessing, channel: Int, dynamics: DynamicsSettings, lowLatency: Boolean) {
        runCatching {
            val mbc = dp.getMbcByChannelIndex(channel)
            val band = mbc.getBand(0)
            band.isEnabled = true
            band.threshold = dynamics.compThresholdDb
            band.ratio = dynamics.compRatio
            band.attackTime = if (lowLatency) 0.001f else dynamics.compAttackSec
            band.releaseTime = if (lowLatency) 0.050f else dynamics.compReleaseSec
            band.kneeWidth = dynamics.compKneeDb
            band.preGain = 0f
            band.postGain = 0f
            mbc.setBand(0, band)
            dp.setMbcByChannelIndex(channel, mbc)
        }.onFailure { Log.w(TAG, "MBC config failed for channel $channel", it) }
    }

    private fun applyLimiter(dp: DynamicsProcessing, channel: Int, lowLatency: Boolean) {
        runCatching {
            val limiter = dp.getLimiterByChannelIndex(channel)
            limiter.isEnabled = true
            limiter.threshold = LIMITER_THRESHOLD_DB
            limiter.ratio = LIMITER_RATIO
            limiter.attackTime = if (lowLatency) LOW_LATENCY_LIMITER_ATTACK_SEC else LIMITER_ATTACK_SEC
            limiter.releaseTime = if (lowLatency) LOW_LATENCY_LIMITER_RELEASE_SEC else LIMITER_RELEASE_SEC
            limiter.postGain = 0f
            dp.setLimiterByChannelIndex(channel, limiter)
        }.onFailure { Log.w(TAG, "Limiter config failed for channel $channel", it) }
    }

    private fun setPostEqBand(dp: DynamicsProcessing, channel: Int, bandIndex: Int, frequencyHz: Float, gainDb: Float) {
        runCatching {
            val band = dp.getPostEqBandByChannelIndex(channel, bandIndex)
            band.isEnabled = true
            band.cutoffFrequency = frequencyHz
            band.gain = gainDb.coerceIn(-12f, 12f)
            dp.setPostEqBandByChannelIndex(channel, bandIndex, band)
        }.onFailure { Log.w(TAG, "PostEQ band $bandIndex config failed for channel $channel", it) }
    }

    /** Output trim in dB, distinct from the phone's system volume. */
    fun setOutputTrim(trimDb: Float) {
        runCatching {
            dynamicsProcessing?.setInputGainAllChannelsTo(trimDb.coerceIn(-12f, 0f))
        }.onFailure { Log.w(TAG, "Output trim failed", it) }
    }
}
