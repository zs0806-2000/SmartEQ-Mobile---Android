package com.smarteq.pro.data.artist

import com.smarteq.pro.data.model.DynamicsSettings

/**
 * Local, fully-offline artist acoustic-signature library. Ported 1:1 from
 * KNOWN_ARTIST_SIGNATURES in the original web app's artistTuningService.ts.
 *
 * This is the library the app falls back to (and, per project requirements,
 * uses by default) when no Gemini API key is configured — Auto-Tune is a
 * fully functional, fully local feature without any network dependency.
 */
data class ArtistAcousticSignature(
    val genre: String,
    val eq15: List<Float>,
    val dynamics: DynamicsSettings,
    val description: String,
)

object ArtistSignatureLibrary {

    val SIGNATURES: Map<String, ArtistAcousticSignature> = mapOf(
        "daft punk" to ArtistAcousticSignature(
            genre = "French House / Synthwave",
            eq15 = listOf(4.0f, 3.5f, 3.0f, 1.5f, 0.0f, -1.0f, -1.5f, -0.5f, 0.5f, 1.5f, 2.5f, 3.0f, 3.5f, 4.0f, 3.5f),
            dynamics = DynamicsSettings(bassDb = 3.5f, trebleDb = 3.0f, compThresholdDb = -20f, compRatio = 6f, compAttackSec = 0.005f, compReleaseSec = 0.15f, compKneeDb = 20f),
            description = "Punchy French Touch sub-bass with pristine analog synth clarity and crisp high-hat air."
        ),
        "taylor swift" to ArtistAcousticSignature(
            genre = "Pop / Folk Pop",
            eq15 = listOf(1.0f, 1.5f, 2.0f, 1.0f, 0.0f, 0.5f, 1.0f, 1.5f, 2.5f, 2.0f, 1.5f, 1.0f, 1.5f, 2.0f, 1.5f),
            dynamics = DynamicsSettings(bassDb = 1.5f, trebleDb = 2.0f, compThresholdDb = -22f, compRatio = 4f, compAttackSec = 0.01f, compReleaseSec = 0.2f, compKneeDb = 15f),
            description = "Shimmering vocal intimacy with smooth acoustic warmth and wide polished stereo image."
        ),
        "kendrick lamar" to ArtistAcousticSignature(
            genre = "Hip Hop / Conscious Rap",
            eq15 = listOf(5.0f, 4.5f, 3.5f, 2.0f, 0.5f, -0.5f, -1.0f, 0.5f, 1.5f, 2.5f, 2.0f, 1.0f, 1.5f, 2.0f, 2.5f),
            dynamics = DynamicsSettings(bassDb = 4.5f, trebleDb = 2.0f, compThresholdDb = -18f, compRatio = 8f, compAttackSec = 0.002f, compReleaseSec = 0.25f, compKneeDb = 25f),
            description = "Deep 808 sub extension with forward vocal presence and crisp snare attack."
        ),
        "drake" to ArtistAcousticSignature(
            genre = "Contemporary R&B / Rap",
            eq15 = listOf(4.5f, 4.0f, 3.0f, 1.5f, 0.5f, 0.0f, -1.0f, -0.5f, 0.5f, 1.5f, 2.0f, 1.5f, 2.0f, 2.5f, 2.0f),
            dynamics = DynamicsSettings(bassDb = 4.0f, trebleDb = 2.0f, compThresholdDb = -20f, compRatio = 6f, compAttackSec = 0.004f, compReleaseSec = 0.2f, compKneeDb = 20f),
            description = "Atmospheric low-end warmth with lush filtered pad depth and smooth vocal EQ."
        ),
        "billie eilish" to ArtistAcousticSignature(
            genre = "Dark Pop / Alt Pop",
            eq15 = listOf(6.0f, 5.0f, 4.0f, 2.0f, 0.0f, -1.0f, -1.5f, -1.0f, 0.0f, 1.0f, 2.0f, 2.5f, 3.0f, 3.5f, 3.0f),
            dynamics = DynamicsSettings(bassDb = 5.0f, trebleDb = 2.5f, compThresholdDb = -24f, compRatio = 5f, compAttackSec = 0.015f, compReleaseSec = 0.3f, compKneeDb = 30f),
            description = "Ultra-deep sub bass response paired with hyper-close whisper vocal presence."
        ),
        "hans zimmer" to ArtistAcousticSignature(
            genre = "Cinematic / Orchestral",
            eq15 = listOf(5.5f, 4.5f, 3.0f, 1.5f, 0.5f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f),
            dynamics = DynamicsSettings(bassDb = 4.0f, trebleDb = 3.5f, compThresholdDb = -28f, compRatio = 3f, compAttackSec = 0.02f, compReleaseSec = 0.4f, compKneeDb = 35f),
            description = "Massive orchestral sub-bass foundation with transparent dynamic headroom and brass shimmer."
        ),
        "metallica" to ArtistAcousticSignature(
            genre = "Heavy Metal / Thrash",
            eq15 = listOf(2.0f, 3.0f, 3.5f, 2.0f, 0.5f, -1.5f, -2.0f, -1.0f, 0.5f, 2.0f, 3.0f, 3.5f, 2.5f, 1.5f, 2.0f),
            dynamics = DynamicsSettings(bassDb = 3.0f, trebleDb = 2.5f, compThresholdDb = -16f, compRatio = 10f, compAttackSec = 0.001f, compReleaseSec = 0.1f, compKneeDb = 10f),
            description = "Scooped mid guitar crunch, punchy double-kick drum transient, and bright cymbal bite."
        ),
        "miles davis" to ArtistAcousticSignature(
            genre = "Jazz / Cool Jazz",
            eq15 = listOf(1.0f, 1.0f, 0.5f, 0.0f, 0.5f, 1.0f, 1.5f, 1.5f, 2.0f, 1.5f, 1.0f, 0.5f, 1.0f, 1.5f, 2.0f),
            dynamics = DynamicsSettings(bassDb = 0.5f, trebleDb = 1.5f, compThresholdDb = -30f, compRatio = 2.5f, compAttackSec = 0.025f, compReleaseSec = 0.35f, compKneeDb = 35f),
            description = "Warm, organic acoustic resonance with silky muted trumpet timbre and natural dynamics."
        ),
        "the weeknd" to ArtistAcousticSignature(
            genre = "Synth Pop / R&B",
            eq15 = listOf(4.0f, 3.5f, 2.5f, 1.0f, -0.5f, -1.0f, -0.5f, 0.5f, 1.5f, 2.5f, 3.0f, 3.0f, 3.5f, 3.0f, 2.5f),
            dynamics = DynamicsSettings(bassDb = 3.5f, trebleDb = 3.0f, compThresholdDb = -21f, compRatio = 7f, compAttackSec = 0.003f, compReleaseSec = 0.18f, compKneeDb = 20f),
            description = "80s analog synth bass punch combined with soaring, vibrant falsetto vocal EQ."
        ),
    )

    val GENERIC = ArtistAcousticSignature(
        genre = "General Audio",
        eq15 = listOf(2.0f, 2.0f, 1.5f, 0.5f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 2.0f, 2.0f, 1.5f, 2.0f, 2.5f, 2.0f),
        dynamics = DynamicsSettings(bassDb = 2.0f, trebleDb = 2.0f, compThresholdDb = -22f, compRatio = 4f, compAttackSec = 0.005f, compReleaseSec = 0.2f, compKneeDb = 20f),
        description = "Balanced frequency profile tailored for vocal clarity and punchy dynamics."
    )

    /** Case-insensitive exact-then-substring match, same fallback order as the web app. */
    fun findForArtist(artistName: String): ArtistAcousticSignature {
        val key = artistName.trim().lowercase()
        SIGNATURES[key]?.let { return it }
        val partial = SIGNATURES.keys.firstOrNull { key.contains(it) || it.contains(key) }
        return partial?.let { SIGNATURES.getValue(it) } ?: GENERIC
    }

    /** Quick-select chips shown in the Auto-Tune screen. */
    val QUICK_ARTISTS = listOf(
        "Daft Punk", "Taylor Swift", "Kendrick Lamar", "Billie Eilish",
        "Hans Zimmer", "Metallica", "The Weeknd"
    )
}
