package com.smarteq.pro.data.repository

import com.smarteq.pro.data.model.TrackMetadata
import com.smarteq.pro.data.model.TrackSource
import com.smarteq.pro.data.remote.spotify.SpotifyApi
import com.smarteq.pro.data.remote.spotify.SpotifyAuthManager
import com.smarteq.pro.data.remote.spotify.SpotifyPlaylist
import com.smarteq.pro.data.security.SecureKeyStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SpotifyRepository(
    private val api: SpotifyApi,
    private val authManager: SpotifyAuthManager,
    private val secureKeyStore: SecureKeyStore,
) {
    val isConnected: Boolean get() = secureKeyStore.isSpotifyConnected()

    fun beginLogin(): Boolean {
        val clientId = secureKeyStore.getSpotifyClientId() ?: return false
        authManager.beginLogin(clientId)
        return true
    }

    suspend fun handleLoginRedirect(uri: android.net.Uri): Result<Unit> {
        val clientId = secureKeyStore.getSpotifyClientId()
            ?: return Result.failure(IllegalStateException("No Spotify Client ID configured"))
        return authManager.handleRedirect(uri, clientId)
    }

    fun disconnect() = authManager.disconnect()

    suspend fun fetchTopArtistNames(): List<String> {
        val bearer = bearerOrNull() ?: return emptyList()
        return runCatching { api.getTopArtists(bearer).items.map { it.name } }.getOrDefault(emptyList())
    }

    suspend fun fetchPlaylists(): List<SpotifyPlaylist> {
        val bearer = bearerOrNull() ?: return emptyList()
        return runCatching { api.getPlaylists(bearer).items }.getOrDefault(emptyList())
    }

    suspend fun fetchCurrentTrack(): TrackMetadata? {
        val bearer = bearerOrNull() ?: return null
        val response = runCatching { api.getCurrentlyPlaying(bearer) }.getOrNull() ?: return null
        val item = response.item ?: return null
        return TrackMetadata(
            title = item.name,
            artist = item.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
            album = item.album?.name,
            source = TrackSource.SPOTIFY,
            albumArtUrl = item.album?.images?.firstOrNull()?.url,
        )
    }

    /** Polls currently-playing every 6s while connected. */
    fun observeCurrentTrack(): Flow<TrackMetadata?> = flow {
        while (true) {
            if (isConnected) emit(fetchCurrentTrack()) else emit(null)
            delay(6_000)
        }
    }

    private suspend fun bearerOrNull(): String? {
        val clientId = secureKeyStore.getSpotifyClientId() ?: return null
        val token = authManager.refreshIfNeeded(clientId) ?: secureKeyStore.getSpotifyAccessToken() ?: return null
        return "Bearer $token"
    }
}
