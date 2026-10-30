package com.smarteq.pro.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted-at-rest storage for the two categories of secret this app ever
 * touches: an optional, user-supplied Gemini API key, and Spotify OAuth
 * tokens obtained via the PKCE flow. Backed by Android Keystore through
 * EncryptedSharedPreferences (AES256-GCM), so the values are unreadable
 * even with root/adb backup access without the hardware-bound key.
 *
 * Nothing here is ever bundled, hardcoded, or transmitted anywhere except
 * directly to the corresponding first-party API (Google's Gemini endpoint,
 * Spotify's token/API endpoints) over TLS, and only when the user has
 * explicitly enabled that connection in Settings.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    // --- Gemini ---
    fun getGeminiApiKey(): String? = prefs.getString(KEY_GEMINI_API_KEY, null)
    fun setGeminiApiKey(value: String?) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(KEY_GEMINI_API_KEY) else putString(KEY_GEMINI_API_KEY, value.trim())
        }.apply()
    }

    // --- Spotify ---
    fun getSpotifyAccessToken(): String? = prefs.getString(KEY_SPOTIFY_ACCESS_TOKEN, null)
    fun getSpotifyRefreshToken(): String? = prefs.getString(KEY_SPOTIFY_REFRESH_TOKEN, null)
    fun getSpotifyTokenExpiryEpochMs(): Long = prefs.getLong(KEY_SPOTIFY_EXPIRY, 0L)
    fun getSpotifyClientId(): String? = prefs.getString(KEY_SPOTIFY_CLIENT_ID, null)

    fun setSpotifyClientId(value: String?) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(KEY_SPOTIFY_CLIENT_ID) else putString(KEY_SPOTIFY_CLIENT_ID, value.trim())
        }.apply()
    }

    fun saveSpotifyTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int) {
        prefs.edit().apply {
            putString(KEY_SPOTIFY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_SPOTIFY_REFRESH_TOKEN, it) }
            putLong(KEY_SPOTIFY_EXPIRY, System.currentTimeMillis() + expiresInSeconds * 1000L)
        }.apply()
    }

    /** Wipes all Spotify tokens immediately — called on manual disconnect. */
    fun clearSpotifyTokens() {
        prefs.edit()
            .remove(KEY_SPOTIFY_ACCESS_TOKEN)
            .remove(KEY_SPOTIFY_REFRESH_TOKEN)
            .remove(KEY_SPOTIFY_EXPIRY)
            .apply()
    }

    fun isSpotifyConnected(): Boolean = !getSpotifyAccessToken().isNullOrBlank()

    // --- App Lock PIN (stored as a salted hash, never plaintext) ---
    fun getAppLockPinHash(): String? = prefs.getString(KEY_APP_LOCK_PIN_HASH, null)
    fun getAppLockPinSalt(): String? = prefs.getString(KEY_APP_LOCK_PIN_SALT, null)
    fun setAppLockPin(hash: String?, salt: String?) {
        prefs.edit().apply {
            if (hash == null || salt == null) {
                remove(KEY_APP_LOCK_PIN_HASH)
                remove(KEY_APP_LOCK_PIN_SALT)
            } else {
                putString(KEY_APP_LOCK_PIN_HASH, hash)
                putString(KEY_APP_LOCK_PIN_SALT, salt)
            }
        }.apply()
    }

    private companion object {
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
        const val KEY_SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
        const val KEY_SPOTIFY_REFRESH_TOKEN = "spotify_refresh_token"
        const val KEY_SPOTIFY_EXPIRY = "spotify_token_expiry"
        const val KEY_SPOTIFY_CLIENT_ID = "spotify_client_id"
        const val KEY_APP_LOCK_PIN_HASH = "app_lock_pin_hash"
        const val KEY_APP_LOCK_PIN_SALT = "app_lock_pin_salt"
    }
}
