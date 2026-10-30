package com.smarteq.pro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.data.remote.spotify.SpotifyAuthManager
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.data.repository.SpotifyRepository
import com.smarteq.pro.data.security.SecureKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val geminiKeyConfigured: Boolean = false,
    val geminiNetworkEnabled: Boolean = false,
    val spotifyClientIdConfigured: Boolean = false,
    val spotifyConnected: Boolean = false,
    val spotifyNetworkEnabled: Boolean = false,
    val resumeOnBoot: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockUseBiometric: Boolean = true,
    val appLockTimeoutSeconds: Int = 0,
    val appLockPinConfigured: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val secureKeyStore: SecureKeyStore,
    private val spotifyRepository: SpotifyRepository,
    val spotifyAuthManager: SpotifyAuthManager,
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    private val toggleGroup = combine(
        settingsRepository.geminiNetworkEnabled,
        settingsRepository.spotifyNetworkEnabled,
        settingsRepository.resumeOnBoot,
        settingsRepository.appLockEnabled,
        settingsRepository.appLockUseBiometric,
    ) { geminiOn, spotifyOn, resumeOnBoot, lockEnabled, lockBiometric ->
        ToggleGroup(geminiOn, spotifyOn, resumeOnBoot, lockEnabled, lockBiometric)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        toggleGroup,
        settingsRepository.appLockTimeoutSeconds,
        _refreshTrigger,
    ) { toggles, lockTimeout, _ ->
        SettingsUiState(
            geminiKeyConfigured = !secureKeyStore.getGeminiApiKey().isNullOrBlank(),
            geminiNetworkEnabled = toggles.geminiOn,
            spotifyClientIdConfigured = !secureKeyStore.getSpotifyClientId().isNullOrBlank(),
            spotifyConnected = spotifyRepository.isConnected,
            spotifyNetworkEnabled = toggles.spotifyOn,
            resumeOnBoot = toggles.resumeOnBoot,
            appLockEnabled = toggles.lockEnabled,
            appLockUseBiometric = toggles.lockBiometric,
            appLockTimeoutSeconds = lockTimeout,
            appLockPinConfigured = secureKeyStore.getAppLockPinHash() != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private data class ToggleGroup(
        val geminiOn: Boolean,
        val spotifyOn: Boolean,
        val resumeOnBoot: Boolean,
        val lockEnabled: Boolean,
        val lockBiometric: Boolean,
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
}
