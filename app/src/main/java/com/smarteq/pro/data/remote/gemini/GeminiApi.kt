package com.smarteq.pro.data.remote.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Thin Retrofit interface over Google's Gemini "generateContent" REST
 * endpoint. Called directly from the device using a user-supplied API key
 * (see SecureKeyStore) — there is no SmartyQ backend server in the
 * request path, matching the "no bundled secrets, no proxy required"
 * decision made for this build.
 *
 * NOTE ON MODEL NAME: the original SmartEQ Pro web app (now SmartyQ) used
 * "gemini-3-flash-preview". Preview model IDs are updated by Google over
 * time; GeminiModel.DEFAULT below is kept as a single named constant
 * (rather than scattered string literals) specifically so it's a one-line
 * change if/when Google renames or retires that preview model. The
 * Settings screen also exposes an advanced "Model override" field for the
 * same reason.
 */
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest,
    ): GeminiGenerateContentResponse
}

object GeminiModel {
    const val DEFAULT = "gemini-3-flash-preview"
    const val BASE_URL = "https://generativelanguage.googleapis.com/"
}

@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: GeminiSchema? = null,
)

@Serializable
data class GeminiSchema(
    val type: String,
    val properties: Map<String, GeminiSchema>? = null,
    val items: GeminiSchema? = null,
    val description: String? = null,
    val required: List<String>? = null,
)

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>? = null,
)

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null)

/** Parsed, validated result of an artist+device tuning generation call. */
@Serializable
data class GeminiArtistTuningResponse(
    val artistEq15: List<Float>,
    val bass: Float,
    val treble: Float,
    val compThreshold: Float,
    val compRatio: Float,
    val compAttack: Float,
    val compRelease: Float,
    val compKnee: Float,
    val explanation: String,
) {
    val eq15: List<Float> get() = artistEq15
}

/** Parsed result of a freeform "type an artist/genre" prompt (mirrors handleAnalyze). */
@Serializable
data class GeminiSmartEqResponse(
    val eq: List<Float>,
    val bass: Float,
    val treble: Float,
    val compressor: GeminiCompressorBlock,
    val explanation: String,
)

@Serializable
data class GeminiCompressorBlock(
    val threshold: Float,
    val ratio: Float,
    val attack: Float,
    val release: Float,
    val knee: Float,
)
