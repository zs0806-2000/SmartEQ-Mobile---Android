package com.smarteq.pro.data.repository

import com.smarteq.pro.data.artist.ArtistSignatureLibrary
import com.smarteq.pro.data.model.AutoEqProfile
import com.smarteq.pro.data.model.CombinedTuningResult
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.TuningSource
import com.smarteq.pro.data.remote.gemini.GeminiArtistTuningResponse
import com.smarteq.pro.data.remote.gemini.GeminiClient
import com.smarteq.pro.data.security.SecureKeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Produces a combined artist + hardware EQ curve. Mirrors
 * generateArtistAndDeviceTuning() from artistTuningService.ts, including
 * the exact -12..12 dB clamp behavior in combineCurves().
 *
 * IMPORTANT (per product requirement): Gemini is entirely optional. This
 * function ALWAYS succeeds using ArtistSignatureLibrary even if no key is
 * configured, the key is invalid, or the network call fails for any
 * reason — the caller never needs to handle a "no AI" failure state.
 */
@Singleton
class ArtistTuningRepository @Inject constructor(
    private val geminiClient: GeminiClient,
    private val secureKeyStore: SecureKeyStore,
) {

    suspend fun generateTuning(
        artistName: String,
        trackTitle: String?,
        autoEqProfile: AutoEqProfile,
        allowGeminiNetworkCall: Boolean,
    ): CombinedTuningResult {
        val cleanArtist = artistName.ifBlank { "Default Artist" }.trim()

        val geminiKey = secureKeyStore.getGeminiApiKey()
        if (allowGeminiNetworkCall && !geminiKey.isNullOrBlank()) {
            val aiResult = runCatching {
                geminiClient.generateArtistTuning(
                    apiKey = geminiKey,
                    artist = cleanArtist,
                    trackTitle = trackTitle,
                    deviceLabel = "${autoEqProfile.brand} ${autoEqProfile.model}",
                    deviceType = autoEqProfile.type.name,
                )
            }.getOrNull()

            if (aiResult != null && aiResult.eq15.size == 15) {
                return buildResult(
                    artist = cleanArtist,
                    trackTitle = trackTitle,
                    autoEqProfile = autoEqProfile,
                    rawEq15 = aiResult.eq15,
                    dynamics = aiResult.toDynamicsSettings(),
                    explanation = aiResult.explanation,
                    source = TuningSource.GEMINI_AI,
                )
            }
            // Falls through to local library on any Gemini failure — the
            // feature must never dead-end the user.
        }

        val signature = ArtistSignatureLibrary.findForArtist(cleanArtist)
        return buildResult(
            artist = cleanArtist,
            trackTitle = trackTitle,
            autoEqProfile = autoEqProfile,
            rawEq15 = signature.eq15,
            dynamics = signature.dynamics,
            explanation = "Tuned for $cleanArtist (${signature.genre}): ${signature.description} " +
                "Combined with AutoEQ curve for ${autoEqProfile.brand} ${autoEqProfile.model}.",
            source = TuningSource.LOCAL_LIBRARY,
        )
    }

    private fun buildResult(
        artist: String,
        trackTitle: String?,
        autoEqProfile: AutoEqProfile,
        rawEq15: List<Float>,
        dynamics: DynamicsSettings,
        explanation: String,
        source: TuningSource,
    ): CombinedTuningResult {
        val combined = combineCurves(rawEq15, autoEqProfile.correction15)
        return CombinedTuningResult(
            artist = artist,
            trackTitle = trackTitle,
            outputDeviceName = "${autoEqProfile.brand} ${autoEqProfile.model}",
            autoEqProfileId = autoEqProfile.id,
            eq15 = combined,
            dynamics = dynamics,
            explanation = explanation,
            source = source,
        )
    }

    /** Exact port of combineCurves() in artistTuningService.ts: sum then clamp to [-12, 12]. */
    private fun combineCurves(artistEq: List<Float>, autoEqCorrection: List<Float>): List<Float> {
        return artistEq.mapIndexed { index, value ->
            val correction = autoEqCorrection.getOrElse(index) { 0f }
            val rawSum = value + correction
            val clamped = max(-12f, min(12f, rawSum))
            round(clamped * 10) / 10f
        }
    }
}

private fun GeminiArtistTuningResponse.toDynamicsSettings() = DynamicsSettings(
    bassDb = bass,
    trebleDb = treble,
    compThresholdDb = compThreshold,
    compRatio = compRatio,
    compAttackSec = compAttack,
    compReleaseSec = compRelease,
    compKneeDb = compKnee,
)
