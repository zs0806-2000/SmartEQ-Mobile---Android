package com.smarteq.pro.audio.engine

import com.smarteq.pro.data.model.BandCount
import com.smarteq.pro.data.model.BandFrequencies
import com.smarteq.pro.data.model.BuiltInPresets
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class EqState(
    val bandCount: BandCount = BandCount.FIFTEEN,
    val bandGainsDb: List<Float> = List(15) { 0f },
    val dynamics: DynamicsSettings = DynamicsSettings.DEFAULT,
    val outputTrimDb: Float = 0f,
    val activePresetId: String? = BuiltInPresets.FLAT.id,
)

/** The adaptive per-track layer: 15 band gains plus a loudness-match gain, both in dB. */
data class AdaptiveLayer(
    val gains15: List<Float> = List(EqCurveMath.CENTERS_15.size) { 0f },
    val loudnessGainDb: Float = 0f,
)

/**
 * The single source of truth for what EQ configuration is active, shared by the UI and
 * the live DSP engine.
 *
 * The user's own curve and the adaptive per-track layer are stored separately and only
 * added together on the way to the engine. Turning adaptive tuning off therefore restores
 * exactly what the user set, and the sliders always show the user's own choices.
 */
class EqStateStore(
    private val sessionRegistry: AudioSessionRegistry,
) {
    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state

    private val _adaptive = MutableStateFlow(AdaptiveLayer())
    val adaptive: StateFlow<AdaptiveLayer> = _adaptive

    /** The 15-band curve actually sent to the engine (user + bass/treble + adaptive). */
    private val _effective15 = MutableStateFlow(List(EqCurveMath.CENTERS_15.size) { 0f })
    val effective15: StateFlow<List<Float>> = _effective15

    init {
        pushToEngine()
    }

    fun setBandCount(bandCount: BandCount) {
        _state.update {
            it.copy(
                bandCount = bandCount,
                bandGainsDb = List(bandCount.count) { i -> it.bandGainsDb.getOrElse(i) { 0f } },
                activePresetId = null,
            )
        }
        pushToEngine()
    }

    fun setBandGain(index: Int, valueDb: Float) {
        _state.update { s ->
            if (index !in s.bandGainsDb.indices) return@update s
            val updated = s.bandGainsDb.toMutableList().also { it[index] = valueDb.coerceIn(-12f, 12f) }
            s.copy(bandGainsDb = updated, activePresetId = null)
        }
        pushToEngine()
    }

    fun updateDynamics(update: (DynamicsSettings) -> DynamicsSettings) {
        _state.update { it.copy(dynamics = update(it.dynamics), activePresetId = null) }
        pushToEngine()
    }

    fun setOutputTrim(trimDb: Float) {
        _state.update { it.copy(outputTrimDb = trimDb) }
        pushToEngine()
    }

    fun applyPreset(preset: EqPreset) {
        _state.update {
            it.copy(
                bandCount = BandCount.fromCount(preset.bandCount),
                bandGainsDb = preset.bandGainsDb,
                dynamics = preset.dynamics,
                activePresetId = preset.id,
            )
        }
        pushToEngine()
    }

    fun applyGeneratedTuning(bandGainsDb15: List<Float>, dynamics: DynamicsSettings) {
        _state.update {
            it.copy(bandCount = BandCount.FIFTEEN, bandGainsDb = bandGainsDb15, dynamics = dynamics, activePresetId = null)
        }
        pushToEngine()
    }

    fun setAdaptiveLayer(gains15: List<Float>, loudnessGainDb: Float) {
        _adaptive.value = AdaptiveLayer(gains15, loudnessGainDb)
        pushToEngine()
    }

    fun clearAdaptiveLayer() = setAdaptiveLayer(List(EqCurveMath.CENTERS_15.size) { 0f }, 0f)

    fun resetToFlat() = applyPreset(BuiltInPresets.FLAT)

    fun restoreSavedBandCount(count: Int) {
        setBandCount(BandCount.fromCount(count))
    }

    fun snapshot(): EqState = _state.value

    @Synchronized
    private fun pushToEngine() {
        val s = _state.value
        val a = _adaptive.value
        val centers = BandFrequencies.forCount(s.bandCount).map { it.toFloat() }
        val manual15 = EqCurveMath.resampleTo15(s.bandGainsDb, centers)
        val final15 = EqCurveMath.mergeLayers(manual15, s.dynamics.bassDb, s.dynamics.trebleDb, a.gains15)
        _effective15.value = final15
        val inputGain = s.outputTrimDb + a.loudnessGainDb + EqCurveMath.headroomDb(final15)
        sessionRegistry.updateConfiguration(final15, s.dynamics, inputGain)
    }
}
