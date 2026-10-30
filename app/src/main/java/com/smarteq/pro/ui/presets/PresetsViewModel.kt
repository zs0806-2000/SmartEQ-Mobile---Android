package com.smarteq.pro.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.audio.engine.EqStateStore
import com.smarteq.pro.data.model.EqPreset
import com.smarteq.pro.data.repository.PresetRepository
import com.smarteq.pro.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetsViewModel(
    private val presetRepository: PresetRepository,
    private val eqStateStore: EqStateStore,
) : ViewModel() {

    val presets: StateFlow<List<EqPreset>> = presetRepository.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activePresetId: StateFlow<String?> = eqStateStore.state
        .map { it.activePresetId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun applyPreset(preset: EqPreset) = eqStateStore.applyPreset(preset)

    fun deletePreset(preset: EqPreset) {
        viewModelScope.launch { presetRepository.delete(preset) }
    }

    fun saveCurrentAsPreset(name: String) {
        if (name.isBlank()) return
        val snapshot = eqStateStore.snapshot()
        val preset = EqPreset(
            id = System.currentTimeMillis().toString(),
            name = name.trim(),
            bandCount = snapshot.bandCount.count,
            bandGainsDb = snapshot.bandGainsDb,
            dynamics = snapshot.dynamics,
        )
        viewModelScope.launch {
            presetRepository.save(preset)
            eqStateStore.applyPreset(preset)
        }
    }

    fun linkToOutputDevice(preset: EqPreset, deviceAddress: String?) {
        viewModelScope.launch { presetRepository.linkToDevice(preset, deviceAddress) }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PresetsViewModel(
                        container.presetRepository,
                        container.eqStateStore,
                    ) as T
                }
            }
    }
}
