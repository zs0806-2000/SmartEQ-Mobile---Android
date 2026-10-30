package com.smarteq.pro.data.remote.gemini

import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All calls in this class are 100% optional from the app's point of view:
 * every caller has a fully-functional local fallback and treats any
 * exception here as "use the local library instead," never as a hard
 * failure surfaced to the user as an error state.
 */
@Singleton
class GeminiClient @Inject constructor(
    private val api: GeminiApi,
    private val json: Json,
) {

    suspend fun generateArtistTuning(
        apiKey: String,
        artist: String,
        trackTitle: String?,
        deviceLabel: String,
        deviceType: String,
        modelOverride: String? = null,
    ): GeminiArtistTuningResponse {
        val prompt = buildString {
            append("You are a world-class audio mastering engineer.\n")
            append("The user is listening to: Artist: \"$artist\"")
            if (!trackTitle.isNullOrBlank()) append(", Track: \"$trackTitle\"")
            append(".\n")
            append("They are listening on headphones/speakers: \"$deviceLabel\" ($deviceType).\n\n")
            append("Calculate the ideal 15-band EQ target for this artist's mixing signature and genre profile.\n")
            append("Standard 15 frequencies: 25Hz, 40Hz, 63Hz, 100Hz, 160Hz, 250Hz, 400Hz, 630Hz, 1kHz, 1.6kHz, 2.5kHz, 4kHz, 6.3kHz, 10kHz, 16kHz.\n\n")
            append("Return ONLY a JSON object matching the schema. Values for artistEq15 must be numbers between -10 and +10 dB.")
        }

        val schema = GeminiSchema(
            type = "OBJECT",
            required = listOf(
                "artistEq15", "bass", "treble", "compThreshold", "compRatio",
                "compAttack", "compRelease", "compKnee", "explanation"
            ),
            properties = mapOf(
                "artistEq15" to GeminiSchema(type = "ARRAY", items = GeminiSchema(type = "NUMBER"), description = "15 target gain values (-10 to +10 dB)"),
                "bass" to GeminiSchema(type = "NUMBER", description = "Bass gain (-10 to +10 dB)"),
                "treble" to GeminiSchema(type = "NUMBER", description = "Treble gain (-10 to +10 dB)"),
                "compThreshold" to GeminiSchema(type = "NUMBER", description = "Threshold -60 to 0 dB"),
                "compRatio" to GeminiSchema(type = "NUMBER", description = "Ratio 1 to 20"),
                "compAttack" to GeminiSchema(type = "NUMBER", description = "Attack 0 to 1 seconds"),
                "compRelease" to GeminiSchema(type = "NUMBER", description = "Release 0 to 1 seconds"),
                "compKnee" to GeminiSchema(type = "NUMBER", description = "Knee 0 to 40 dB"),
                "explanation" to GeminiSchema(type = "STRING", description = "Brief 2-sentence explanation of artist + AutoEQ synergy."),
            )
        )

        val responseText = callGemini(apiKey, prompt, schema, modelOverride)
        return json.decodeFromString(GeminiArtistTuningResponse.serializer(), responseText)
    }

    suspend fun generateSmartEqFromPrompt(
        apiKey: String,
        userPrompt: String,
        bandCount: Int,
        bandLabels: List<String>,
        modelOverride: String? = null,
    ): GeminiSmartEqResponse {
        val prompt = "You are an expert audio engineer. The user wants to listen to: \"$userPrompt\". " +
            "Provide the ideal $bandCount-band EQ settings (gains from -12 to +12 dB) and advanced settings " +
            "(bass, treble, compressor threshold, compressor ratio, attack, release, knee) to maximize the " +
            "listening experience for this specific music. Return ONLY a JSON object."

        val schema = GeminiSchema(
            type = "OBJECT",
            required = listOf("eq", "bass", "treble", "compressor", "explanation"),
            properties = mapOf(
                "eq" to GeminiSchema(type = "ARRAY", items = GeminiSchema(type = "NUMBER"), description = "$bandCount values between -12 and 12 for frequencies: ${bandLabels.joinToString(", ")}"),
                "bass" to GeminiSchema(type = "NUMBER", description = "Bass gain -12 to 12"),
                "treble" to GeminiSchema(type = "NUMBER", description = "Treble gain -12 to 12"),
                "compressor" to GeminiSchema(
                    type = "OBJECT",
                    properties = mapOf(
                        "threshold" to GeminiSchema(type = "NUMBER", description = "Threshold -60 to 0"),
                        "ratio" to GeminiSchema(type = "NUMBER", description = "Ratio 1 to 20"),
                        "attack" to GeminiSchema(type = "NUMBER", description = "Attack time 0 to 1 seconds"),
                        "release" to GeminiSchema(type = "NUMBER", description = "Release time 0 to 1 seconds"),
                        "knee" to GeminiSchema(type = "NUMBER", description = "Knee 0 to 40 dB"),
                    )
                ),
                "explanation" to GeminiSchema(type = "STRING", description = "Brief explanation of why these settings work for this music."),
            )
        )

        val responseText = callGemini(apiKey, prompt, schema, modelOverride)
        return json.decodeFromString(GeminiSmartEqResponse.serializer(), responseText)
    }

    suspend fun generateAudioPersonality(
        apiKey: String,
        topArtistNames: List<String>,
        modelOverride: String? = null,
    ): String {
        val prompt = "You are an expert music critic and audio engineer. The user's top Spotify artists are: " +
            "${topArtistNames.joinToString(", ")}. Write a fun, insightful 2-paragraph analysis of their music " +
            "taste, genre preferences, and 'audio personality'. What kind of sound signature do they likely prefer?"

        val response = api.generateContent(
            model = modelOverride?.ifBlank { null } ?: GeminiModel.DEFAULT,
            apiKey = apiKey,
            request = GeminiGenerateContentRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt))))),
        )
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini returned no text content")
    }

    private suspend fun callGemini(
        apiKey: String,
        prompt: String,
        schema: GeminiSchema,
        modelOverride: String?,
    ): String {
        val response = api.generateContent(
            model = modelOverride?.ifBlank { null } ?: GeminiModel.DEFAULT,
            apiKey = apiKey,
            request = GeminiGenerateContentRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                ),
            ),
        )
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini returned no JSON content")
    }
}
