package com.smarteq.pro.data.remote.spotify

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.smarteq.pro.data.security.SecureKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Implements Spotify's Authorization Code with PKCE flow — the flow
 * Spotify's own documentation designates for apps (like this one) that
 * cannot safely hold a client secret. NO client secret is requested,
 * stored, or used anywhere in this app; only a Client ID the user
 * generates themselves in the Spotify Developer Dashboard and enters in
 * Settings (see SPOTIFY_SETUP.md for the exact steps).
 *
 * This entire manager is inert until the user explicitly taps
 * "Connect Spotify" — nothing here runs automatically or on app start.
 */
@Singleton
class SpotifyAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeyStore: SecureKeyStore,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    private var pendingCodeVerifier: String? = null

    companion object {
        const val REDIRECT_URI = "smarteqpro://spotify-callback"
        private const val SCOPES = "user-read-private user-top-read " +
            "user-read-currently-playing user-read-playback-state " +
            "playlist-read-private playlist-read-collaborative"
    }

    /** Launches the system browser (Custom Tabs) to Spotify's login/consent page. */
    fun beginLogin(clientId: String) {
        val verifier = generateCodeVerifier()
        pendingCodeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)

        val authUri = "https://accounts.spotify.com/authorize".toUri().buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()

        CustomTabsIntent.Builder().build().launchUrl(context, authUri)
    }

    /** Call from MainActivity's onNewIntent when REDIRECT_URI comes back. */
    suspend fun handleRedirect(redirectUri: Uri, clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val code = redirectUri.getQueryParameter("code")
            ?: return@withContext Result.failure(IllegalStateException("No authorization code in redirect"))
        val verifier = pendingCodeVerifier
            ?: return@withContext Result.failure(IllegalStateException("No pending PKCE verifier — login flow was not started by this app instance"))

        runCatching {
            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", clientId)
                .add("code_verifier", verifier)
                .build()
            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(body)
                .build()
            val responseBody = okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("Spotify token exchange failed: HTTP ${resp.code}")
                resp.body?.string() ?: error("Empty token response")
            }
            val token = json.decodeFromString(SpotifyTokenResponse.serializer(), responseBody)
            secureKeyStore.saveSpotifyTokens(token.access_token, token.refresh_token, token.expires_in)
            pendingCodeVerifier = null
        }
    }

    suspend fun refreshIfNeeded(clientId: String): String? = withContext(Dispatchers.IO) {
        val expiry = secureKeyStore.getSpotifyTokenExpiryEpochMs()
        val current = secureKeyStore.getSpotifyAccessToken()
        if (current != null && System.currentTimeMillis() < expiry - 30_000) return@withContext current

        val refreshToken = secureKeyStore.getSpotifyRefreshToken() ?: return@withContext null
        return@withContext runCatching {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .build()
            val request = Request.Builder().url("https://accounts.spotify.com/api/token").post(body).build()
            val responseBody = okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("Spotify token refresh failed: HTTP ${resp.code}")
                resp.body?.string() ?: error("Empty refresh response")
            }
            val token = json.decodeFromString(SpotifyTokenResponse.serializer(), responseBody)
            secureKeyStore.saveSpotifyTokens(token.access_token, token.refresh_token ?: refreshToken, token.expires_in)
            token.access_token
        }.getOrNull()
    }

    fun disconnect() = secureKeyStore.clearSpotifyTokens()

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
