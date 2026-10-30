package com.smarteq.pro.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.audio.engine.AudioSessionRegistry
import com.smarteq.pro.audio.engine.EqState
import com.smarteq.pro.audio.engine.EqStateStore
import com.smarteq.pro.data.model.BandCount
import com.smarteq.pro.data.model.EqEngineMode
import com.smarteq.pro.data.model.EqEngineStatus
import com.smarteq.pro.data.model.EqPreset
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.service.EqualizerForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EqualizerUiState(
    val engineEnabled: Boolean = true,
    val engineMode: EqEngineMode = EqEngineMode.DEFAULT_PER_SESSION,
    val engineStatus: EqEngineStatus = EqEngineStatus(),
    val eq: EqState = EqState(),
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val eqStateStore: EqStateStore,
    private val sessionRegistry: AudioSessionRegistry,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    val uiState: StateFlow<EqualizerUiState> = combine(
        eqStateStore.state,
        settingsRepository.eqEngineEnabled,
        settingsRepository.eqEngineMode,
        sessionRegistry.status,
    ) { eq, enabled, mode, status ->
        EqualizerUiState(engineEnabled = enabled, engineMode = mode, engineStatus = status, eq = eq)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EqualizerUiState())

    init {
        viewModelScope.launch {
            val savedCount = settingsRepository.bandCount.first()
            eqStateStore.restoreSavedBandCount(savedCount)
        }
    }

    fun setEngineEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEqEngineEnabled(enabled)
            if (enabled) EqualizerForegroundService.ensureStarted(appContext) else EqualizerForegroundService.stop(appContext)
        }
    }

    fun setEngineMode(mode: EqEngineMode) {
        viewModelScope.launch { settingsRepository.setEqEngineMode(mode) }
    }

    fun setBandCount(bandCount: BandCount) {
        eqStateStore.setBandCount(bandCount)
        viewModelScope.launch { settingsRepository.setBandCount(bandCount.count) }
    }

    fun setBandGain(index: Int, valueDb: Float) = eqStateStore.setBandGain(index, valueDb)

    fun setDynamics(update: (com.smarteq.pro.data.model.DynamicsSettings) -> com.smarteq.pro.data.model.DynamicsSettings) =
        eqStateStore.updateDynamics(update)

    fun setOutputTrim(trimDb: Float) = eqStateStore.setOutputTrim(trimDb)

    fun applyPreset(preset: EqPreset) = eqStateStore.applyPreset(preset)

    fun resetToFlat() = eqStateStore.resetToFlat()

    fun currentSnapshotForSaving(): EqState = eqStateStore.snapshot()
}
