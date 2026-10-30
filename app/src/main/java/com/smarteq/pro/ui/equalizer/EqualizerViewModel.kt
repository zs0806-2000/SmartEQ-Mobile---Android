package com.smarteq.pro.ui.equalizer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.audio.engine.AudioRoutingManager
import com.smarteq.pro.audio.engine.AudioSessionRegistry
import com.smarteq.pro.audio.engine.EqState
import com.smarteq.pro.audio.engine.EqStateStore
import com.smarteq.pro.data.model.AudioOutputDevice
import com.smarteq.pro.data.model.BandCount
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.EqEngineMode
import com.smarteq.pro.data.model.EqEngineStatus
import com.smarteq.pro.data.model.EqPreset
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.di.AppContainer
import com.smarteq.pro.service.EqualizerForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqualizerUiState(
    val engineEnabled: Boolean = true,
    val engineMode: EqEngineMode = EqEngineMode.UNIVERSAL_GLOBAL,
    val engineStatus: EqEngineStatus = EqEngineStatus(),
    val outputDevice: AudioOutputDevice = AudioOutputDevice(),
    val isLowLatencyActive: Boolean = true,
    val eq: EqState = EqState(),
)

class EqualizerViewModel(
    private val eqStateStore: EqStateStore,
    private val sessionRegistry: AudioSessionRegistry,
    private val settingsRepository: SettingsRepository,
    private val audioRoutingManager: AudioRoutingManager,
    private val appContext: Context,
) : ViewModel() {

    private val engineSettingsFlow = combine(
        settingsRepository.eqEngineEnabled,
        settingsRepository.eqEngineMode,
        sessionRegistry.status,
    ) { enabled, mode, status ->
        Triple(enabled, mode, status)
    }

    val uiState: StateFlow<EqualizerUiState> = combine(
        eqStateStore.state,
        engineSettingsFlow,
        audioRoutingManager.currentOutputDevice,
        audioRoutingManager.lowLatencyModeEnabled,
    ) { eq, (enabled, mode, status), device, lowLatency ->
        EqualizerUiState(
            engineEnabled = enabled,
            engineMode = mode,
            engineStatus = status,
            outputDevice = device,
            isLowLatencyActive = lowLatency && device.isBluetooth,
            eq = eq,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EqualizerUiState())

    init {
        viewModelScope.launch {
            val savedCount = settingsRepository.bandCount.first()
            eqStateStore.restoreSavedBandCount(savedCount)
            val lowLatencyPref = settingsRepository.bluetoothLowLatencyEnabled.first()
            audioRoutingManager.setLowLatencyMode(lowLatencyPref)
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

    fun setLowLatencyMode(enabled: Boolean) {
        audioRoutingManager.setLowLatencyMode(enabled)
        viewModelScope.launch { settingsRepository.setBluetoothLowLatencyEnabled(enabled) }
    }

    fun setBandCount(bandCount: BandCount) {
        eqStateStore.setBandCount(bandCount)
        viewModelScope.launch { settingsRepository.setBandCount(bandCount.count) }
    }

    fun setBandGain(index: Int, valueDb: Float) = eqStateStore.setBandGain(index, valueDb)

    fun setDynamics(update: (DynamicsSettings) -> DynamicsSettings) =
        eqStateStore.updateDynamics(update)

    fun setOutputTrim(trimDb: Float) = eqStateStore.setOutputTrim(trimDb)

    fun applyPreset(preset: EqPreset) = eqStateStore.applyPreset(preset)

    fun resetToFlat() = eqStateStore.resetToFlat()

    fun currentSnapshotForSaving(): EqState = eqStateStore.snapshot()

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EqualizerViewModel(
                        container.eqStateStore,
                        container.sessionRegistry,
                        container.settingsRepository,
                        container.audioRoutingManager,
                        container.appContext
                    ) as T
                }
            }
    }
}
