package com.smarteq.pro.audio.engine

import android.media.audiofx.DynamicsProcessing
import android.util.Log
import com.smarteq.pro.data.model.DynamicsSettings

/**
 * Wraps android.media.audiofx.DynamicsProcessing (API 28+) for one audio session,
 * with the platform Equalizer as a fallback on devices that refuse DynamicsProcessing.
 *
 * The engine always runs a fixed 15-band EQ internally ([EqCurveMath.CENTERS_15]).
 * The user's 5/10/15-band curve, the bass/treble controls and the adaptive per-track
 * layer are all merged into those 15 bands by [EqCurveMath] before they reach here,
 * so changing the band count never tears down and rebuilds the effect.
 *
 * Units follow the platform API: compressor and limiter times are milliseconds,
 * EQ band "cutoff" is the band's TOP edge in Hz (bands must ascend), gains in dB.
 * Reference: https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing
 */
class SmartEqDspEngine(
    private val audioSessionId: Int,
) {
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var fallbackEqualizer: android.media.audiofx.Equalizer? = null
    private var channelCount: Int = 2
    private var currentLowLatency: Boolean = false

    private companion object {
        const val TAG = "SmartEqDspEngine"
        const val VARIANT_FAVOR_FREQUENCY_RESOLUTION = 0
        const val VARIANT_FAVOR_SPEED = 1
        const val LIMITER_THRESHOLD_DB = -1f
        const val LIMITER_RATIO = 10f
        const val LIMITER_ATTACK_MS = 1f
        const val LIMITER_RELEASE_MS = 60f
        const val MBC_TOP_HZ = 20000f
    }

    var isAttached: Boolean = false
        private set

    /** True when only the basic platform Equalizer could be attached (fewer bands, no compressor). */
    var isFallback: Boolean = false
        private set

    fun attach(channels: Int = 2, lowLatency: Boolean = false): Boolean {
        try {
            channelCount = channels.coerceAtLeast(1)
            currentLowLatency = lowLatency
            val variant = if (lowLatency) VARIANT_FAVOR_SPEED else VARIANT_FAVOR_FREQUENCY_RESOLUTION
            val config = DynamicsProcessing.Config.Builder(
                variant,
                channelCount,
                /* preEqInUse = */ false, /* preEqBandCount = */ 0,
                /* mbcInUse = */ true, /* mbcBandCount = */ 1,
                /* postEqInUse = */ true, /* postEqBandCount = */ EqCurveMath.CENTERS_15.size,
                /* limiterInUse = */ true,
            ).build()

            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply { enabled = true }
            isAttached = true
            isFallback = false
            Log.i(TAG, "Attached DynamicsProcessing to session $audioSessionId")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "DynamicsProcessing unavailable for session $audioSessionId; trying Equalizer", e)
        }

        return try {
            fallbackEqualizer = android.media.audiofx.Equalizer(0, audioSessionId).apply { enabled = true }
            isAttached = true
            isFallback = true
            Log.i(TAG, "Attached fallback Equalizer to session $audioSessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "No equalizer could be attached to session $audioSessionId", e)
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
        }.onFailure { Log.w(TAG, "Error releasing Equalizer for session $audioSessionId", it) }
        fallbackEqualizer = null
        isAttached = false
    }

    /**
     * Applies a complete configuration.
     * @param gains15 final per-band gains for [EqCurveMath.CENTERS_15], already merged and clamped.
     * @param inputGainDb gain applied before the EQ (output trim + loudness match + headroom).
     */
    fun apply(gains15: List<Float>, dynamics: DynamicsSettings, inputGainDb: Float, lowLatency: Boolean = currentLowLatency) {
        if (dynamicsProcessing != null && lowLatency != currentLowLatency) {
            release()
            if (!attach(channelCount, lowLatency)) return
        }
        val dp = dynamicsProcessing
        if (dp != null) {
            runCatching { dp.setInputGainAllChannelsTo(inputGainDb.coerceIn(-24f, 6f)) }
                .onFailure { Log.w(TAG, "Input gain failed", it) }
            val edges = EqCurveMath.BAND_TOP_EDGES_15
            for (channel in 0 until channelCount) {
                applyMbc(dp, channel, dynamics)
                applyLimiter(dp, channel)
                for (i in edges.indices) {
                    setPostEqBand(dp, channel, i, edges[i], gains15.getOrElse(i) { 0f })
                }
            }
            return
        }

        val eq = fallbackEqualizer ?: return
        runCatching {
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()
            for (i in 0 until numBands) {
                // The platform reports each band's centre in milliHertz.
                val centerHz = eq.getCenterFreq(i.toShort()) / 1000f
                val gainDb = EqCurveMath.sampleCurveAt(gains15, centerHz) + inputGainDb.coerceAtMost(0f)
                val millibels = (gainDb * 100).toInt().coerceIn(minLevel, maxLevel).toShort()
                eq.setBandLevel(i.toShort(), millibels)
            }
        }.onFailure { Log.w(TAG, "Fallback Equalizer configuration failed", it) }
    }

    private fun applyMbc(dp: DynamicsProcessing, channel: Int, dynamics: DynamicsSettings) {
        runCatching {
            val mbc = dp.getMbcByChannelIndex(channel)
            val band = mbc.getBand(0)
            // A band must stay enabled: a disabled band is documented as muting its range.
            // "Compressor off" is expressed as ratio 1:1 with the threshold at 0 dBFS.
            val bypass = dynamics.compRatio <= 1.01f
            band.isEnabled = true
            band.cutoffFrequency = MBC_TOP_HZ
            band.threshold = if (bypass) 0f else dynamics.compThresholdDb.coerceIn(-60f, 0f)
            band.ratio = if (bypass) 1f else dynamics.compRatio.coerceIn(1f, 20f)
            band.attackTime = (dynamics.compAttackSec * 1000f).coerceIn(0.5f, 500f)
            band.releaseTime = (dynamics.compReleaseSec * 1000f).coerceIn(10f, 2000f)
            band.kneeWidth = dynamics.compKneeDb.coerceIn(0f, 40f)
            band.preGain = 0f
            band.postGain = 0f
            mbc.setBand(0, band)
            dp.setMbcByChannelIndex(channel, mbc)
        }.onFailure { Log.w(TAG, "MBC config failed for channel $channel", it) }
    }

    private fun applyLimiter(dp: DynamicsProcessing, channel: Int) {
        runCatching {
            val limiter = dp.getLimiterByChannelIndex(channel)
            limiter.isEnabled = true
            limiter.threshold = LIMITER_THRESHOLD_DB
            limiter.ratio = LIMITER_RATIO
            limiter.attackTime = LIMITER_ATTACK_MS
            limiter.releaseTime = LIMITER_RELEASE_MS
            limiter.postGain = 0f
            dp.setLimiterByChannelIndex(channel, limiter)
        }.onFailure { Log.w(TAG, "Limiter config failed for channel $channel", it) }
    }

    private fun setPostEqBand(dp: DynamicsProcessing, channel: Int, bandIndex: Int, topEdgeHz: Float, gainDb: Float) {
        runCatching {
            val band = dp.getPostEqBandByChannelIndex(channel, bandIndex)
            band.isEnabled = true
            band.cutoffFrequency = topEdgeHz
            band.gain = gainDb.coerceIn(-12f, 12f)
            dp.setPostEqBandByChannelIndex(channel, bandIndex, band)
        }.onFailure { Log.w(TAG, "PostEQ band $bandIndex config failed for channel $channel", it) }
    }
}
