package com.smarteq.pro.audio.engine

import com.smarteq.pro.data.model.BandCount
import com.smarteq.pro.data.model.BuiltInPresets
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class EqState(
    val bandCount: BandCount = BandCount.FIFTEEN,
    val bandGainsDb: List<Float> = List(15) { 0f },
    val dynamics: DynamicsSettings = DynamicsSettings.DEFAULT,
    val outputTrimDb: Float = 0f,
    val activePresetId: String? = BuiltInPresets.FLAT.id,
)

/**
 * The single source of truth for "what EQ configuration is currently
 * active," shared by the Equalizer screen, the Auto-Tune screen, and the
 * live DSP engine. Both screens read and write through here instead of
 * keeping independent copies, so an Auto-Tune result immediately shows up
 * on the Equalizer screen's sliders and vice versa.
 */
@Singleton
class EqStateStore @Inject constructor(
    private val sessionRegistry: AudioSessionRegistry,
) {
    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state

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
        sessionRegistry.setOutputTrim(trimDb)
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

    fun resetToFlat() = applyPreset(BuiltInPresets.FLAT)

    fun restoreSavedBandCount(count: Int) {
        setBandCount(BandCount.fromCount(count))
    }

    fun snapshot(): EqState = _state.value

    private fun pushToEngine() {
        val s = _state.value
        sessionRegistry.updateConfiguration(s.bandGainsDb, s.dynamics, s.bandCount.count)
    }
}
