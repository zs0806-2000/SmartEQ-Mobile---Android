package com.smarteq.pro.data.model

import kotlinx.serialization.Serializable

/** Number of active bands the user has selected — mirrors the web app's 5/10/15 toggle. */
enum class BandCount(val count: Int) {
    FIVE(5), TEN(10), FIFTEEN(15);

    companion object {
        fun fromCount(count: Int): BandCount = entries.firstOrNull { it.count == count } ?: FIFTEEN
    }
}

/** Standard band center frequencies (Hz), matching the web app's FREQ_SETS. */
object BandFrequencies {
    val FIVE = listOf(63, 250, 1000, 4000, 16000)
    val TEN = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val FIFTEEN = listOf(25, 40, 63, 100, 160, 250, 400, 630, 1000, 1600, 2500, 4000, 6300, 10000, 16000)

    fun forCount(bandCount: BandCount): List<Int> = when (bandCount) {
        BandCount.FIVE -> FIVE
        BandCount.TEN -> TEN
        BandCount.FIFTEEN -> FIFTEEN
    }

    val FIVE_LABELS = listOf("63", "250", "1k", "4k", "16k")
    val TEN_LABELS = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    val FIFTEEN_LABELS = listOf(
        "25", "40", "63", "100", "160", "250", "400", "630",
        "1k", "1.6k", "2.5k", "4k", "6.3k", "10k", "16k"
    )

    fun labelsForCount(bandCount: BandCount): List<String> = when (bandCount) {
        BandCount.FIVE -> FIVE_LABELS
        BandCount.TEN -> TEN_LABELS
        BandCount.FIFTEEN -> FIFTEEN_LABELS
    }
}

/** Full dynamics-processing state — one-to-one with the web app's compressor knobs. */
@Serializable
data class DynamicsSettings(
    val bassDb: Float = 0f,
    val trebleDb: Float = 0f,
    val compThresholdDb: Float = -24f,
    val compRatio: Float = 12f,
    val compAttackSec: Float = 0.003f,
    val compReleaseSec: Float = 0.25f,
    val compKneeDb: Float = 30f,
) {
    companion object {
        val DEFAULT = DynamicsSettings()
    }
}

/** A full EQ + dynamics configuration — equivalent of the web app's `Preset` interface. */
@Serializable
data class EqPreset(
    val id: String,
    val name: String,
    val bandCount: Int,
    val bandGainsDb: List<Float>,
    val dynamics: DynamicsSettings,
    val linkedOutputDeviceAddress: String? = null,
    val isBuiltIn: Boolean = false,
)

/** Built-in factory presets, ported 1:1 from DEFAULT_PRESETS in App.tsx. */
object BuiltInPresets {
    val FLAT = EqPreset(
        id = "flat",
        name = "Flat",
        bandCount = 15,
        bandGainsDb = List(15) { 0f },
        dynamics = DynamicsSettings(bassDb = 0f, trebleDb = 0f, compThresholdDb = -24f, compRatio = 12f, compAttackSec = 0.003f, compReleaseSec = 0.25f, compKneeDb = 30f),
        isBuiltIn = true
    )
    val ROCK = EqPreset(
        id = "rock",
        name = "Rock",
        bandCount = 15,
        bandGainsDb = listOf(4f, 3f, 2f, 1f, -1f, -2f, -3f, -2f, -1f, 1f, 2f, 3f, 4f, 4f, 4f),
        dynamics = DynamicsSettings(bassDb = 4f, trebleDb = 3f, compThresholdDb = -20f, compRatio = 4f, compAttackSec = 0.01f, compReleaseSec = 0.1f, compKneeDb = 10f),
        isBuiltIn = true
    )
    val ELECTRONIC = EqPreset(
        id = "electronic",
        name = "Electronic",
        bandCount = 15,
        bandGainsDb = listOf(6f, 5f, 4f, 2f, 0f, -2f, -4f, -2f, 0f, 2f, 4f, 5f, 6f, 6f, 6f),
        dynamics = DynamicsSettings(bassDb = 6f, trebleDb = 5f, compThresholdDb = -24f, compRatio = 8f, compAttackSec = 0.005f, compReleaseSec = 0.3f, compKneeDb = 20f),
        isBuiltIn = true
    )

    val ALL = listOf(FLAT, ROCK, ELECTRONIC)
}

/** Metadata for whatever track/source is currently active, from any source. */
@Serializable
data class TrackMetadata(
    val title: String,
    val artist: String,
    val album: String? = null,
    val source: TrackSource,
    val albumArtUrl: String? = null,
)

@Serializable
enum class TrackSource { SPOTIFY, LOCAL_FILE, MANUAL, SYSTEM_SESSION }

/** A known headphone/speaker AutoEQ correction profile — mirrors AutoEQProfile in autoTune.ts. */
@Serializable
data class AutoEqProfile(
    val id: String,
    val brand: String,
    val model: String,
    val type: AutoEqDeviceType,
    val description: String,
    /** 15-band correction offsets in dB, same frequency order as BandFrequencies.FIFTEEN. */
    val correction15: List<Float>,
)

@Serializable
enum class AutoEqDeviceType { IN_EAR, OVER_EAR, ON_EAR, SPEAKERS, PHONE }

/** Result of combining an artist's mix signature with a device's AutoEQ correction curve. */
data class CombinedTuningResult(
    val artist: String,
    val trackTitle: String?,
    val outputDeviceName: String,
    val autoEqProfileId: String,
    val eq15: List<Float>,
    val dynamics: DynamicsSettings,
    val explanation: String,
    val source: TuningSource,
)

enum class TuningSource { LOCAL_LIBRARY, GEMINI_AI }

/** Which real-time EQ engine mode is active. */
enum class EqEngineMode { UNIVERSAL_GLOBAL, DEFAULT_PER_SESSION, LEGACY_GLOBAL }

/** Live status of the audio engine, observed by the UI. */
data class EqEngineStatus(
    val isRunning: Boolean = false,
    val mode: EqEngineMode = EqEngineMode.UNIVERSAL_GLOBAL,
    val activeSessions: List<ActiveAudioSession> = emptyList(),
)

data class ActiveAudioSession(
    val sessionId: Int,
    val sourcePackageName: String,
    val sourceAppLabel: String,
)
