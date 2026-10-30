package com.smarteq.pro.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smarteq.pro.data.model.EqEngineMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "smarteq_settings")

/**
 * Central home for every user-facing on/off switch in the app, including
 * the two independently-toggleable network permissions required by
 * project policy: Gemini calls and Spotify calls can each be turned off
 * without affecting the other or any core (fully local) functionality.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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
