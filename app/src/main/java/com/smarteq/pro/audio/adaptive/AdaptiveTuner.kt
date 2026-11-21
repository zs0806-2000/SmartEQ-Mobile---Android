package com.smarteq.pro.audio.adaptive

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.smarteq.pro.audio.engine.AudioRoutingManager
import com.smarteq.pro.audio.engine.AudioSessionRegistry
import com.smarteq.pro.audio.engine.EqCurveMath
import com.smarteq.pro.audio.engine.EqStateStore
import com.smarteq.pro.data.artist.ArtistSignatureLibrary
import com.smarteq.pro.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AdaptivePhase { OFF, WAITING_FOR_MUSIC, ANALYZING, APPLIED, TRACK_INFO_ONLY, UNAVAILABLE }

data class AdaptiveStatus(
    val phase: AdaptivePhase = AdaptivePhase.OFF,
    val trackLabel: String? = null,
    val correction15: List<Float> = List(TonalBalance.BAND_COUNT) { 0f },
    val loudnessGainDb: Float = 0f,
    val tracksLearned: Int = 0,
    val outputLabel: String = "",
    /** Plain-language note shown under the status, e.g. why listening is unavailable. */
    val note: String? = null,
)

@Serializable
private data class LearnedProfile(
    val target: List<Float?> = emptyList(),
    val tracks: Int = 0,
    val targetRmsDb: Float? = null,
    /** 0 = the analyser hears the raw track, 1 = it hears our processed output. */
    val postEffect: Float = 0.5f,
)

private data class AdaptiveConfig(
    val enabled: Boolean,
    val useTrackInfo: Boolean,
    val listen: Boolean,
    val strength: Float,
    val loudness: Boolean,
)

/**
 * Re-tunes the adaptive EQ layer for every track.
 *
 *  1. When a new track starts (known from track info, if allowed), the layer ramps to a mild
 *     starting point: the artist's profile from the built-in library if the artist is in it,
 *     otherwise neutral.
 *  2. If listening is allowed, it skips the first 3 seconds, measures about 10 seconds of the
 *     track's tonal balance, and ramps in a correction toward the balance you usually listen to
 *     on this output device (see [TonalBalance]). It learns from every track.
 *  3. Without track info, it re-measures every 25 seconds and adjusts slowly instead.
 *
 * All changes are ramped in 0.5 dB steps so they are not heard as jumps. Corrections are
 * limited to ±4 dB per band. The user's own EQ is never modified; this is a separate layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdaptiveTuner(
    private val appContext: Context,
    private val settings: SettingsRepository,
    private val nowPlaying: NowPlayingRepository,
    private val registry: AudioSessionRegistry,
    private val eqStateStore: EqStateStore,
    private val routing: AudioRoutingManager,
) {
    private val _status = MutableStateFlow(AdaptiveStatus())
    val status: StateFlow<AdaptiveStatus> = _status

    private var scope: CoroutineScope? = null
    private val analyzer = SpectrumAnalyzer()
    private val json = Json { ignoreUnknownKeys = true }
    private val profiles = mutableMapOf<String, LearnedProfile>()
    private var profilesLoaded = false

    fun start() {
        if (scope != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = s
        s.launch { loop() }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        analyzer.stop()
        eqStateStore.clearAdaptiveLayer()
        _status.value = AdaptiveStatus()
    }

    /** Forgets everything learned for all output devices. */
    fun resetLearning() {
        profiles.clear()
        scope?.launch { settings.setAdaptiveProfilesJson(null) }
        _status.value = _status.value.copy(tracksLearned = 0)
    }

    private suspend fun loop() {
        loadProfiles()
        val config = combine(
            settings.adaptiveEnabled, settings.adaptiveUseTrackInfo, settings.adaptiveListen,
            settings.adaptiveStrength, settings.adaptiveLoudness,
        ) { e, t, l, st, ld -> AdaptiveConfig(e, t, l, st, ld) }

        // Restart the per-track routine whenever settings, the track, play state or output change.
        combine(
            config,
            nowPlaying.nowPlaying.map { np -> np?.let { it.trackKey to it.isPlaying } }.distinctUntilChanged(),
            routing.currentOutputDevice.map { "${it.type}|${it.name}" }.distinctUntilChanged(),
        ) { c, track, output -> Triple(c, track, output) }
            .collectLatest { (c, track, output) ->
                runCatching { runFor(c, track?.first, track?.second ?: false, output) }
                    .onFailure { if (it !is kotlinx.coroutines.CancellationException) Log.w(TAG, "Adaptive run failed", it) else throw it }
            }
    }

    private suspend fun runFor(c: AdaptiveConfig, trackKey: String?, playing: Boolean, outputKey: String) {
        if (!c.enabled) {
            analyzer.stop()
            rampTo(List(TonalBalance.BAND_COUNT) { 0f }, 0f)
            _status.value = AdaptiveStatus(phase = AdaptivePhase.OFF)
            return
        }
        val np = nowPlaying.nowPlaying.value
        val trackLabel = np?.let { listOf(it.artist, it.title).filter(String::isNotBlank).joinToString(" – ") }
        val outputLabel = routing.currentOutputDevice.value.name
        val profile = profiles[outputKey] ?: LearnedProfile()
        val haveTrackInfo = c.useTrackInfo && nowPlaying.listenerConnected.value && trackKey != null

        if (haveTrackInfo && !playing) {
            analyzer.stop()
            _status.value = AdaptiveStatus(AdaptivePhase.WAITING_FOR_MUSIC, trackLabel, eqStateStore.adaptive.value.gains15,
                tracksLearned = profile.tracks, outputLabel = outputLabel)
            return
        }

        // Step 1: starting point from track info.
        val hint = if (haveTrackInfo && np != null) artistHint(np.artist, c.strength) else zeros()
        rampTo(hint, 0f)

        val canListen = c.listen && hasRecordPermission()
        if (!canListen) {
            analyzer.stop()
            _status.value = AdaptiveStatus(
                phase = if (haveTrackInfo) AdaptivePhase.TRACK_INFO_ONLY else AdaptivePhase.UNAVAILABLE,
                trackLabel = trackLabel, correction15 = hint, tracksLearned = profile.tracks, outputLabel = outputLabel,
                note = when {
                    c.listen -> "Allow Microphone access to let SmartyQ analyse the sound."
                    !haveTrackInfo -> "Turn on track info or sound analysis to start adaptive tuning."
                    else -> null
                },
            )
            return
        }

        val session = registry.analysisSessionFor(np?.packageName)
        if (!analyzer.start(session)) {
            _status.value = AdaptiveStatus(AdaptivePhase.UNAVAILABLE, trackLabel, hint, tracksLearned = profile.tracks,
                outputLabel = outputLabel, note = "This phone did not allow sound analysis for the current app.")
            return
        }

        if (haveTrackInfo) {
            analyseOnce(c, trackLabel, outputKey, outputLabel, hint, skipMs = 3_000, slow = false)
        } else {
            // No track boundaries known: keep re-measuring and adjust gently.
            while (scope?.isActive == true) {
                analyseOnce(c, null, outputKey, outputLabel, hint, skipMs = 0, slow = true)
                delay(25_000)
            }
        }
    }

    private suspend fun analyseOnce(
        c: AdaptiveConfig, trackLabel: String?, outputKey: String, outputLabel: String,
        hint: List<Float>, skipMs: Long, slow: Boolean,
    ) {
        var profile = profiles[outputKey] ?: LearnedProfile()
        _status.value = AdaptiveStatus(AdaptivePhase.ANALYZING, trackLabel, eqStateStore.adaptive.value.gains15,
            eqStateStore.adaptive.value.loudnessGainDb, profile.tracks, outputLabel)
        delay(skipMs)
        analyzer.reset()
        val appliedDuring = eqStateStore.effective15.value
        val inputGainDuring = eqStateStore.adaptive.value.loudnessGainDb
        val m = waitForMeasurement(maxMs = 12_000, minFrames = 40) ?: run {
            _status.value = _status.value.copy(phase = AdaptivePhase.WAITING_FOR_MUSIC, note = "Waiting for audio to analyse.")
            return
        }

        val shape = TonalBalance.removeOwnEq(m.shapeDb, appliedDuring, profile.postEffect)
        val target = profile.target.takeIf { it.size == TonalBalance.BAND_COUNT }
            ?.map { it ?: Float.NaN }?.toFloatArray()
        val correction = if (target != null) TonalBalance.correction(shape, target, c.strength, profile.tracks) else zeros()

        val trackRms = m.rmsDb?.let { it - profile.postEffect * inputGainDuring }
        val loudness = if (c.loudness && trackRms != null && profile.targetRmsDb != null && profile.tracks >= 3) {
            TonalBalance.loudnessGain(trackRms, profile.targetRmsDb!!, c.strength)
        } else 0f

        // Hint carries less weight once we have a real measurement.
        val newLayer = EqCurveMath.CENTERS_15.indices.map { i ->
            EqCurveMath.roundTenth((correction[i] + 0.5f * hint[i]).coerceIn(-TonalBalance.MAX_CORRECTION_DB, TonalBalance.MAX_CORRECTION_DB))
        }
        val finalLayer = if (slow) blend(eqStateStore.adaptive.value.gains15, newLayer, 0.35f) else newLayer
        val before = eqStateStore.adaptive.value.gains15
        rampTo(finalLayer, loudness)

        // Learn from this track.
        val newTarget = TonalBalance.learn(target, profile.tracks, shape)
        val newRms = when {
            trackRms == null -> profile.targetRmsDb
            profile.targetRmsDb == null -> trackRms
            else -> profile.targetRmsDb!! + (trackRms - profile.targetRmsDb!!) / minOf(profile.tracks + 1, 40)
        }
        profile = profile.copy(target = newTarget.map { if (it.isNaN()) null else it }, tracks = profile.tracks + 1, targetRmsDb = newRms)

        _status.value = AdaptiveStatus(AdaptivePhase.APPLIED, trackLabel, finalLayer, loudness, profile.tracks, outputLabel,
            note = if (profile.tracks < 3) "Still learning your usual sound on this output; corrections stay small for the first few songs." else null)

        // Learn whether the analyser hears our output, from how the measurement moved.
        val delta = finalLayer.indices.map { finalLayer[it] - before.getOrElse(it) { 0f } }
        if (TonalBalance.maxAbs(delta) >= 1.5f) {
            delay(800)
            analyzer.reset()
            val after = waitForMeasurement(maxMs = 5_000, minFrames = 25)
            if (after != null) {
                TonalBalance.estimatePostEffect(m.shapeDb, after.shapeDb, delta)?.let { est ->
                    profile = profile.copy(postEffect = (0.8f * profile.postEffect + 0.2f * est).coerceIn(0f, 1f))
                }
            }
        }
        profiles[outputKey] = profile
        saveProfiles()
    }

    private suspend fun waitForMeasurement(maxMs: Long, minFrames: Int): SpectrumMeasurement? {
        var waited = 0L
        while (waited < maxMs && analyzer.framesSoFar() < minFrames * 2) {
            delay(250); waited += 250
        }
        return analyzer.takeMeasurement(minFrames)
    }

    /** Moves the adaptive layer to [target] in 0.5 dB steps every 80 ms. */
    private suspend fun rampTo(target: List<Float>, loudnessDb: Float) {
        var cur = eqStateStore.adaptive.value
        while (true) {
            val next = EqCurveMath.stepToward(cur.gains15, target, 0.5f)
            val d = loudnessDb - cur.loudnessGainDb
            val nextGain = if (kotlin.math.abs(d) <= 0.5f) loudnessDb else cur.loudnessGainDb + 0.5f * kotlin.math.sign(d)
            eqStateStore.setAdaptiveLayer(next, nextGain)
            cur = eqStateStore.adaptive.value
            if (next == target.map { it } && nextGain == loudnessDb) break
            delay(80)
        }
    }

    private fun artistHint(artist: String, strength: Float): List<Float> {
        if (artist.isBlank()) return zeros()
        val sig = ArtistSignatureLibrary.findForArtist(artist)
        if (sig === ArtistSignatureLibrary.GENERIC) return zeros()
        // The library curves are "taste" curves of up to ±7 dB; use a fraction as a starting point.
        return sig.eq15.map { EqCurveMath.roundTenth((it * 0.35f * strength).coerceIn(-2.5f, 2.5f)) }
    }

    private fun blend(a: List<Float>, b: List<Float>, t: Float) =
        a.indices.map { EqCurveMath.roundTenth(a[it] + (b.getOrElse(it) { 0f } - a[it]) * t) }

    private fun zeros() = List(TonalBalance.BAND_COUNT) { 0f }

    private fun hasRecordPermission() =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private suspend fun loadProfiles() {
        if (profilesLoaded) return
        profilesLoaded = true
        val raw = settings.adaptiveProfilesJson.first() ?: return
        runCatching { json.decodeFromString<Map<String, LearnedProfile>>(raw) }
            .onSuccess { profiles.putAll(it) }
            .onFailure { Log.w(TAG, "Discarding unreadable adaptive profiles", it) }
    }

    private suspend fun saveProfiles() {
        settings.setAdaptiveProfilesJson(json.encodeToString(profiles.toMap()))
    }

    private companion object {
        const val TAG = "AdaptiveTuner"
    }
}
