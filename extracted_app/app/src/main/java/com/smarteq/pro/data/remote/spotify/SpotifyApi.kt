package com.smarteq.pro.data.remote.spotify

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SpotifyApi {
    @GET("v1/me/top/artists?limit=20")
    suspend fun getTopArtists(@Header("Authorization") bearer: String): SpotifyTopArtistsResponse

    @GET("v1/me/player/currently-playing")
    suspend fun getCurrentlyPlaying(@Header("Authorization") bearer: String): SpotifyCurrentlyPlayingResponse?

    @GET("v1/me/playlists?limit=20")
    suspend fun getPlaylists(@Header("Authorization") bearer: String): SpotifyPlaylistsResponse
}

interface SpotifyAccountsApi {
    companion object { const val BASE_URL = "https://accounts.spotify.com/" }
}

@Serializable
data class SpotifyTopArtistsResponse(val items: List<SpotifyArtist> = emptyList())

@Serializable
data class SpotifyArtist(val id: String, val name: String)

@Serializable
data class SpotifyCurrentlyPlayingResponse(
    val item: SpotifyTrack? = null,
    val is_playing: Boolean = false,
)

@Serializable
data class SpotifyTrack(
    val name: String,
    val artists: List<SpotifyArtist> = emptyList(),
    val album: SpotifyAlbum? = null,
)

@Serializable
data class SpotifyAlbum(
    val name: String? = null,
    val images: List<SpotifyImage> = emptyList(),
)

@Serializable
data class SpotifyImage(val url: String)

@Serializable
data class SpotifyPlaylistsResponse(val items: List<SpotifyPlaylist> = emptyList())

@Serializable
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val tracks: SpotifyPlaylistTrackCount? = null,
)

@Serializable
data class SpotifyPlaylistTrackCount(val total: Int = 0)

@Serializable
data class SpotifyTokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Int,
    val token_type: String = "Bearer",
)
