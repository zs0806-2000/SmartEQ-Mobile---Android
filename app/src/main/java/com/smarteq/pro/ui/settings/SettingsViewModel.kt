package com.smarteq.pro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.audio.engine.AudioRoutingManager
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.data.repository.SpotifyRepository
import com.smarteq.pro.data.security.SecureKeyStore
import com.smarteq.pro.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.smarteq.pro.ui.theme.AppFont
import com.smarteq.pro.ui.theme.ThemeMode
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val geminiKeyConfigured: Boolean = false,
    val geminiNetworkEnabled: Boolean = false,
    val spotifyClientIdConfigured: Boolean = false,
    val spotifyConnected: Boolean = false,
    val spotifyNetworkEnabled: Boolean = false,
    val bluetoothLowLatencyEnabled: Boolean = true,
    val resumeOnBoot: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockUseBiometric: Boolean = true,
    val appLockTimeoutSeconds: Int = 0,
    val appLockPinConfigured: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val secureKeyStore: SecureKeyStore,
    private val spotifyRepository: SpotifyRepository,
    private val audioRoutingManager: AudioRoutingManager,
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    private val toggleGroup = combine(
        settingsRepository.geminiNetworkEnabled,
        settingsRepository.spotifyNetworkEnabled,
        settingsRepository.bluetoothLowLatencyEnabled,
        settingsRepository.resumeOnBoot,
        settingsRepository.appLockEnabled,
    ) { geminiOn, spotifyOn, lowLatency, resumeOnBoot, lockEnabled ->
        ToggleGroup(geminiOn, spotifyOn, lowLatency, resumeOnBoot, lockEnabled)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        toggleGroup,
        settingsRepository.appLockUseBiometric,
        settingsRepository.appLockTimeoutSeconds,
        _refreshTrigger,
    ) { toggles, lockBiometric, lockTimeout, _ ->
        SettingsUiState(
            geminiKeyConfigured = !secureKeyStore.getGeminiApiKey().isNullOrBlank(),
            geminiNetworkEnabled = toggles.geminiOn,
            spotifyClientIdConfigured = !secureKeyStore.getSpotifyClientId().isNullOrBlank(),
            spotifyConnected = spotifyRepository.isConnected,
            spotifyNetworkEnabled = toggles.spotifyOn,
            bluetoothLowLatencyEnabled = toggles.lowLatency,
            resumeOnBoot = toggles.resumeOnBoot,
            appLockEnabled = toggles.lockEnabled,
            appLockUseBiometric = lockBiometric,
            appLockTimeoutSeconds = lockTimeout,
            appLockPinConfigured = secureKeyStore.getAppLockPinHash() != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val appFont: StateFlow<AppFont> = settingsRepository.appFontId.map { AppFont.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppFont.DEFAULT)
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeId.map { ThemeMode.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setAppFont(font: AppFont) {
        viewModelScope.launch { settingsRepository.setAppFontId(font.id) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeModeId(mode.id) }
    }

    private data class ToggleGroup(
        val geminiOn: Boolean,
        val spotifyOn: Boolean,
        val lowLatency: Boolean,
        val resumeOnBoot: Boolean,
        val lockEnabled: Boolean,
    )

    fun setGeminiApiKey(key: String) {
        secureKeyStore.setGeminiApiKey(key.ifBlank { null })
        refresh()
    }

    fun clearGeminiApiKey() {
        secureKeyStore.setGeminiApiKey(null)
        viewModelScope.launch { settingsRepository.setGeminiNetworkEnabled(false) }
        refresh()
    }

    fun setGeminiNetworkEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGeminiNetworkEnabled(enabled) }
    }

    fun setSpotifyClientId(clientId: String) {
        secureKeyStore.setSpotifyClientId(clientId.ifBlank { null })
        refresh()
    }

    fun setSpotifyNetworkEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSpotifyNetworkEnabled(enabled) }
    }

    fun setBluetoothLowLatency(enabled: Boolean) {
        audioRoutingManager.setLowLatencyMode(enabled)
        viewModelScope.launch { settingsRepository.setBluetoothLowLatencyEnabled(enabled) }
    }

    fun beginSpotifyLogin(): Boolean = spotifyRepository.beginLogin()

    fun handleSpotifyRedirect(uri: android.net.Uri) {
        viewModelScope.launch {
            spotifyRepository.handleLoginRedirect(uri)
            refresh()
        }
    }

    fun disconnectSpotify() {
        spotifyRepository.disconnect()
        viewModelScope.launch { settingsRepository.setSpotifyNetworkEnabled(false) }
        refresh()
    }

    fun setResumeOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setResumeOnBoot(enabled) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }
    }

    fun setAppLockUseBiometric(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAppLockUseBiometric(enabled) }
    }

    fun setAppLockTimeoutSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setAppLockTimeoutSeconds(seconds) }
    }

    private fun refresh() {
        _refreshTrigger.value += 1
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        container.settingsRepository,
                        container.secureKeyStore,
                        container.spotifyRepository,
                        container.audioRoutingManager,
                    ) as T
                }
            }
    }
}
