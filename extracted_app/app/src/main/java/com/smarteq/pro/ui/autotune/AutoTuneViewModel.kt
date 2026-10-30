package com.smarteq.pro.ui.autotune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.audio.engine.EqStateStore
import com.smarteq.pro.data.artist.ArtistSignatureLibrary
import com.smarteq.pro.data.autoeq.AutoEqDatabase
import com.smarteq.pro.data.model.AutoEqProfile
import com.smarteq.pro.data.model.CombinedTuningResult
import com.smarteq.pro.data.model.TrackMetadata
import com.smarteq.pro.data.model.TrackSource
import com.smarteq.pro.data.repository.ArtistTuningRepository
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.data.repository.SpotifyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AutoTuneUiState(
    val currentTrack: TrackMetadata? = TrackMetadata(
        title = "Get Lucky", artist = "Daft Punk", album = "Random Access Memories", source = TrackSource.MANUAL
    ),
    val selectedAutoEqProfile: AutoEqProfile = AutoEqDatabase.PROFILES.first(),
    val autoTuneEnabled: Boolean = true,
    val isTuning: Boolean = false,
    val activeResult: CombinedTuningResult? = null,
    val geminiAvailable: Boolean = false,
    val spotifyConnected: Boolean = false,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AutoTuneViewModel @Inject constructor(
    private val artistTuningRepository: ArtistTuningRepository,
    private val settingsRepository: SettingsRepository,
    private val spotifyRepository: SpotifyRepository,
    private val eqStateStore: EqStateStore,
    private val localMediaMetadataReader: com.smarteq.pro.data.repository.LocalMediaMetadataReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutoTuneUiState())
    val uiState: StateFlow<AutoTuneUiState> = _uiState

    /** Emits a fresh generated result whenever one completes, for the Equalizer screen to consume. */
    private val _lastGeneratedResult = MutableStateFlow<CombinedTuningResult?>(null)
    val lastGeneratedResult: StateFlow<CombinedTuningResult?> = _lastGeneratedResult

    val quickArtists = ArtistSignatureLibrary.QUICK_ARTISTS
    val autoEqProfiles = AutoEqDatabase.PROFILES

    init {
        settingsRepository.autoTuneEnabled
            .onEach { enabled -> _uiState.update { it.copy(autoTuneEnabled = enabled) } }
            .launchIn(viewModelScope)

        settingsRepository.geminiNetworkEnabled
            .onEach { geminiOn -> _uiState.update { it.copy(geminiAvailable = geminiOn) } }
            .launchIn(viewModelScope)

        settingsRepository.spotifyNetworkEnabled
            .onEach { spotifyOn ->
                _uiState.update { it.copy(spotifyConnected = spotifyOn && spotifyRepository.isConnected) }
            }
            .launchIn(viewModelScope)

        // Observe system-wide active media sessions (Spotify, YouTube Music, Apple Music, VLC, etc.)
        com.smarteq.pro.service.MediaSessionListenerService.currentTrack
            .onEach { track -> track?.let { setTrackFromExternalSource(it) } }
            .launchIn(viewModelScope)

        // Only polls Spotify's "currently playing" endpoint while the user
        // has both connected an account AND left the network toggle on.
        settingsRepository.spotifyNetworkEnabled
            .flatMapLatest { enabled ->
                if (enabled && spotifyRepository.isConnected) spotifyRepository.observeCurrentTrack()
                else kotlinx.coroutines.flow.flowOf(null)
            }
            .onEach { track -> track?.let { setTrackFromExternalSource(it) } }
            .launchIn(viewModelScope)
    }

    fun loadLocalAudioFile(uri: android.net.Uri, displayName: String) {
        viewModelScope.launch {
            val metadata = localMediaMetadataReader.read(uri, displayName)
            setTrackFromExternalSource(metadata)
        }
    }

    fun setAutoTuneEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoTuneEnabled(enabled) }
    }

    fun selectAutoEqProfile(profile: AutoEqProfile) {
        _uiState.update { it.copy(selectedAutoEqProfile = profile) }
        triggerAutoTune()
    }

    fun setManualArtist(artist: String, trackTitle: String?) {
        _uiState.update {
            it.copy(currentTrack = TrackMetadata(artist = artist, title = trackTitle ?: "Featured Track", source = TrackSource.MANUAL))
        }
        triggerAutoTune()
    }

    private fun setTrackFromExternalSource(track: TrackMetadata) {
        val current = _uiState.value.currentTrack
        if (current?.artist == track.artist && current.title == track.title) return
        _uiState.update { it.copy(currentTrack = track) }
        if (_uiState.value.autoTuneEnabled) triggerAutoTune()
    }

    fun triggerAutoTune() {
        val state = _uiState.value
        val artist = state.currentTrack?.artist ?: "Daft Punk"
        val title = state.currentTrack?.title
        viewModelScope.launch {
            _uiState.update { it.copy(isTuning = true) }
            val allowGemini = settingsRepository.geminiNetworkEnabled.first()
            val result = artistTuningRepository.generateTuning(
                artistName = artist,
                trackTitle = title,
                autoEqProfile = state.selectedAutoEqProfile,
                allowGeminiNetworkCall = allowGemini,
            )
            _uiState.update { it.copy(isTuning = false, activeResult = result) }
            _lastGeneratedResult.update { result }
            eqStateStore.applyGeneratedTuning(result.eq15, result.dynamics)
        }
    }
}
