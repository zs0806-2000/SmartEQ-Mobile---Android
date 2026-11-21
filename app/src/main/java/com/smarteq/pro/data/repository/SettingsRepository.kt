package com.smarteq.pro.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smarteq.pro.data.model.EqEngineMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "smarteq_settings")

/**
 * Settings repository managing persistence of user preferences, network switches,
 * Bluetooth low latency mode, and security configurations.
 */
class SettingsRepository(
    private val context: Context,
) {
    private object Keys {
        val EQ_ENGINE_MODE = stringPreferencesKey("eq_engine_mode")
        val EQ_ENGINE_ENABLED = booleanPreferencesKey("eq_engine_enabled")
        val GEMINI_NETWORK_ENABLED = booleanPreferencesKey("gemini_network_enabled")
        val SPOTIFY_NETWORK_ENABLED = booleanPreferencesKey("spotify_network_enabled")
        val AUTO_TUNE_ENABLED = booleanPreferencesKey("auto_tune_enabled")
        val RESUME_ON_BOOT = booleanPreferencesKey("resume_on_boot")
        val BAND_COUNT = intPreferencesKey("band_count")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TIMEOUT_SECONDS = intPreferencesKey("app_lock_timeout_seconds")
        val APP_LOCK_USE_BIOMETRIC = booleanPreferencesKey("app_lock_use_biometric")
        val GEMINI_MODEL_OVERRIDE = stringPreferencesKey("gemini_model_override")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val BLUETOOTH_LOW_LATENCY = booleanPreferencesKey("bluetooth_low_latency")
        val ADAPTIVE_ENABLED = booleanPreferencesKey("adaptive_enabled")
        val ADAPTIVE_USE_TRACK_INFO = booleanPreferencesKey("adaptive_use_track_info")
        val ADAPTIVE_LISTEN = booleanPreferencesKey("adaptive_listen")
        val ADAPTIVE_STRENGTH = floatPreferencesKey("adaptive_strength")
        val ADAPTIVE_LOUDNESS = booleanPreferencesKey("adaptive_loudness")
        val ADAPTIVE_PROFILES = stringPreferencesKey("adaptive_profiles_json")
    }

    // ---- Adaptive per-track tuning. Everything is off until the user turns it on. ----

    val adaptiveEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ADAPTIVE_ENABLED] ?: false }
    suspend fun setAdaptiveEnabled(v: Boolean) { context.dataStore.edit { it[Keys.ADAPTIVE_ENABLED] = v } }

    /** Use the playing track's title/artist (needs notification access). */
    val adaptiveUseTrackInfo: Flow<Boolean> = context.dataStore.data.map { it[Keys.ADAPTIVE_USE_TRACK_INFO] ?: false }
    suspend fun setAdaptiveUseTrackInfo(v: Boolean) { context.dataStore.edit { it[Keys.ADAPTIVE_USE_TRACK_INFO] = v } }

    /** Analyse the sound itself (needs the Microphone permission for Android's Visualizer). */
    val adaptiveListen: Flow<Boolean> = context.dataStore.data.map { it[Keys.ADAPTIVE_LISTEN] ?: false }
    suspend fun setAdaptiveListen(v: Boolean) { context.dataStore.edit { it[Keys.ADAPTIVE_LISTEN] = v } }

    /** 0..1, how strongly corrections are applied. */
    val adaptiveStrength: Flow<Float> = context.dataStore.data.map { it[Keys.ADAPTIVE_STRENGTH] ?: 0.6f }
    suspend fun setAdaptiveStrength(v: Float) { context.dataStore.edit { it[Keys.ADAPTIVE_STRENGTH] = v.coerceIn(0f, 1f) } }

    /** Even out loudness between songs. Off by default (experimental). */
    val adaptiveLoudness: Flow<Boolean> = context.dataStore.data.map { it[Keys.ADAPTIVE_LOUDNESS] ?: false }
    suspend fun setAdaptiveLoudness(v: Boolean) { context.dataStore.edit { it[Keys.ADAPTIVE_LOUDNESS] = v } }

    /** Learned tonal-balance targets per output device, as JSON. Stays on this device. */
    val adaptiveProfilesJson: Flow<String?> = context.dataStore.data.map { it[Keys.ADAPTIVE_PROFILES] }
    suspend fun setAdaptiveProfilesJson(json: String?) {
        context.dataStore.edit { if (json == null) it.remove(Keys.ADAPTIVE_PROFILES) else it[Keys.ADAPTIVE_PROFILES] = json }
    }

    val eqEngineMode: Flow<EqEngineMode> = context.dataStore.data.map {
        runCatching { EqEngineMode.valueOf(it[Keys.EQ_ENGINE_MODE] ?: EqEngineMode.UNIVERSAL_GLOBAL.name) }
            .getOrDefault(EqEngineMode.UNIVERSAL_GLOBAL)
    }
    suspend fun setEqEngineMode(mode: EqEngineMode) {
        context.dataStore.edit { it[Keys.EQ_ENGINE_MODE] = mode.name }
    }

    val eqEngineEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EQ_ENGINE_ENABLED] ?: true }
    suspend fun setEqEngineEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQ_ENGINE_ENABLED] = enabled }
    }

    val bluetoothLowLatencyEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BLUETOOTH_LOW_LATENCY] ?: false }
    suspend fun setBluetoothLowLatencyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BLUETOOTH_LOW_LATENCY] = enabled }
    }

    /** Off by default — Gemini is opt-in even after a key is entered. */
    val geminiNetworkEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.GEMINI_NETWORK_ENABLED] ?: false }
    suspend fun setGeminiNetworkEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GEMINI_NETWORK_ENABLED] = enabled }
    }

    /** Off by default — Spotify is opt-in even after a Client ID is entered. */
    val spotifyNetworkEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SPOTIFY_NETWORK_ENABLED] ?: false }
    suspend fun setSpotifyNetworkEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SPOTIFY_NETWORK_ENABLED] = enabled }
    }

    val autoTuneEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_TUNE_ENABLED] ?: true }
    suspend fun setAutoTuneEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_TUNE_ENABLED] = enabled }
    }

    val resumeOnBoot: Flow<Boolean> = context.dataStore.data.map { it[Keys.RESUME_ON_BOOT] ?: false }
    suspend fun setResumeOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_ON_BOOT] = enabled }
    }

    val bandCount: Flow<Int> = context.dataStore.data.map { it[Keys.BAND_COUNT] ?: 15 }
    suspend fun setBandCount(count: Int) {
        context.dataStore.edit { it[Keys.BAND_COUNT] = count }
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }
    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    /** 0 = lock immediately on background; otherwise grace period in seconds. */
    val appLockTimeoutSeconds: Flow<Int> = context.dataStore.data.map { it[Keys.APP_LOCK_TIMEOUT_SECONDS] ?: 0 }
    suspend fun setAppLockTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.APP_LOCK_TIMEOUT_SECONDS] = seconds }
    }

    val appLockUseBiometric: Flow<Boolean> = context.dataStore.data.map { it[Keys.APP_LOCK_USE_BIOMETRIC] ?: true }
    suspend fun setAppLockUseBiometric(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_USE_BIOMETRIC] = enabled }
    }

    val geminiModelOverride: Flow<String?> = context.dataStore.data.map { it[Keys.GEMINI_MODEL_OVERRIDE] }
    suspend fun setGeminiModelOverride(model: String?) {
        context.dataStore.edit {
            if (model.isNullOrBlank()) it.remove(Keys.GEMINI_MODEL_OVERRIDE) else it[Keys.GEMINI_MODEL_OVERRIDE] = model
        }
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }
}
