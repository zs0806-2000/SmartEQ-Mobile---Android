package com.smarteq.pro.audio.engine

import android.media.audiofx.DynamicsProcessing
import android.util.Log
import com.smarteq.pro.data.model.BandFrequencies
import com.smarteq.pro.data.model.DynamicsSettings

/**
 * =============================================================================
 * THE MOST IMPORTANT FILE TO MANUALLY VERIFY BEFORE YOUR FIRST DEVICE BUILD.
 *
 * This wraps android.media.audiofx.DynamicsProcessing (API 28+), the same
 * framework effect Poweramp EQ and Wavelet use for non-root, system-wide
 * equalization. The method names and Config.Builder parameter order below
 * reflect the documented, stable shape of that API as I understand it, but
 * I was not able to compile or run this against the live Android SDK in the
 * environment I wrote it in (no Android SDK / no device / no emulator
 * available there — see the chat for the full disclosure). Before you rely
 * on this in production:
 *   1. Open android.media.audiofx.DynamicsProcessing in Android Studio
 *      (Ctrl/Cmd-click into the class) for your compileSdk 37 sources and
 *      diff method names/signatures against what's used here.
 *   2. Build and run on a real device with a compatible source app playing
 *      audio, and confirm gain changes are audible before shipping.
 * Reference: https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing
 * =============================================================================
 *
 * Signal path per session, chosen to mirror the original web app's chain
 * (compressor -> bass shelf -> treble shelf -> 15 peaking bands -> gain):
 *   MBC (1 band, whole-spectrum dynamics compressor)
 *     -> PostEQ (bandCount + 2 bands: index 0 = bass-shelf approximation,
 *        indices 1..bandCount = the user's graphic EQ bands,
 *        index bandCount+1 = treble-shelf approximation)
 *     -> Limiter (safety ceiling — new vs. the original web app, added to
 *        prevent clipping when multiple bands are boosted simultaneously;
 *        disclosed to the user in Help, not a silent behavior change)
 *
 * PreEQ is intentionally left disabled (0 bands) since DynamicsProcessing's
 * fixed stage order is PreEQ -> MBC -> PostEQ -> Limiter, and putting our
 * EQ content in PostEQ is what makes compression happen *before* EQ,
 * matching the original signal flow.
 *
 * "Bass" and "Treble" are approximated as extra peaking bands near the
 * frequency extremes rather than true shelving filters, because
 * DynamicsProcessing's EqBand type is a parametric (peaking) band, not a
 * shelf. This is documented to the user in Help > "Bass, Treble & Dynamics"
 * rather than silently pretended to be identical to a real shelf filter.
 */
class SmartEqDspEngine(
    private val audioSessionId: Int,
) {
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var fallbackEqualizer: android.media.audiofx.Equalizer? = null
    private var fallbackBassBoost: android.media.audiofx.BassBoost? = null
    private var channelCount: Int = 2
    private var currentBandCount: Int = 15

    private companion object {
        const val TAG = "SmartEqDspEngine"
        const val BASS_APPROX_FREQ_HZ = 55f
        const val TREBLE_APPROX_FREQ_HZ = 13000f
        const val LIMITER_THRESHOLD_DB = -0.5f
        const val LIMITER_RATIO = 20f
        const val LIMITER_ATTACK_SEC = 0.001f
        const val LIMITER_RELEASE_SEC = 0.05f
    }

    /** True once the engine has successfully attached to this session. */
    var isAttached: Boolean = false
        private set

    fun attach(bandCount: Int, channels: Int = 2): Boolean {
        try {
            channelCount = channels.coerceAtLeast(1)
            currentBandCount = bandCount
            val totalPostEqBands = bandCount + 2 // + bass-approx + treble-approx

            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
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
            Log.i(TAG, "Attached DynamicsProcessing to session $audioSessionId ($totalPostEqBands post-EQ bands)")
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
    fun apply(bandGainsDb: List<Float>, dynamics: DynamicsSettings, bandCount: Int) {
        val dp = dynamicsProcessing
        if (dp != null) {
            if (bandCount != currentBandCount) {
                release()
                if (!attach(bandCount, channelCount)) return
            }
            val dp2 = dynamicsProcessing ?: return
            val frequencies = BandFrequencies.forCount(com.smarteq.pro.data.model.BandCount.fromCount(bandCount))

            for (channel in 0 until channelCount) {
                applyMbc(dp2, channel, dynamics)
                applyLimiter(dp2, channel)

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

    private fun applyMbc(dp: DynamicsProcessing, channel: Int, dynamics: DynamicsSettings) {
        runCatching {
            val mbc = dp.getMbcByChannelIndex(channel, 0)
            mbc.isEnabled = true
            mbc.threshold = dynamics.compThresholdDb
            mbc.ratio = dynamics.compRatio
            mbc.attackTime = dynamics.compAttackSec
            mbc.releaseTime = dynamics.compReleaseSec
            mbc.kneeWidth = dynamics.compKneeDb
            mbc.preGain = 0f
            mbc.postGain = 0f
            dp.setMbcByChannelIndex(channel, 0, mbc)
        }.onFailure { Log.w(TAG, "MBC config failed for channel $channel", it) }
    }

    private fun applyLimiter(dp: DynamicsProcessing, channel: Int) {
        runCatching {
            val limiter = dp.getLimiterByChannelIndex(channel)
            limiter.isEnabled = true
            limiter.threshold = LIMITER_THRESHOLD_DB
            limiter.ratio = LIMITER_RATIO
            limiter.attackTime = LIMITER_ATTACK_SEC
            limiter.releaseTime = LIMITER_RELEASE_SEC
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

    /** Output trim in dB, distinct from the phone's system volume — see Help copy. */
    fun setOutputTrim(trimDb: Float) {
        runCatching { dynamicsProcessing?.inputGainAllChannelsTo = trimDb.coerceIn(-12f, 0f) }
            .onFailure { Log.w(TAG, "Output trim failed", it) }
    }
}
