package com.smarteq.pro.di

import android.content.Context
import com.smarteq.pro.audio.engine.AudioRoutingManager
import com.smarteq.pro.audio.engine.AudioSessionRegistry
import com.smarteq.pro.audio.engine.EqStateStore
import com.smarteq.pro.data.local.SmartEqDatabase
import com.smarteq.pro.data.remote.gemini.GeminiApi
import com.smarteq.pro.data.remote.gemini.GeminiClient
import com.smarteq.pro.data.remote.gemini.GeminiModel
import com.smarteq.pro.data.remote.spotify.SpotifyApi
import com.smarteq.pro.data.remote.spotify.SpotifyAuthManager
import com.smarteq.pro.data.repository.ArtistTuningRepository
import com.smarteq.pro.data.repository.LocalMediaMetadataReader
import com.smarteq.pro.data.repository.PresetRepository
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.data.repository.SpotifyRepository
import com.smarteq.pro.data.security.SecureKeyStore
import com.smarteq.pro.ui.applock.AppLockManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val appContext: Context
    val settingsRepository: SettingsRepository
    val secureKeyStore: SecureKeyStore
    val audioRoutingManager: AudioRoutingManager
    val sessionRegistry: AudioSessionRegistry
    val eqStateStore: EqStateStore
    val presetRepository: PresetRepository
    val geminiClient: GeminiClient
    val spotifyRepository: SpotifyRepository
    val artistTuningRepository: ArtistTuningRepository
    val appLockManager: AppLockManager
    val localMediaMetadataReader: LocalMediaMetadataReader
}

class DefaultAppContainer(
    override val appContext: Context,
) : AppContainer {

    override val secureKeyStore: SecureKeyStore by lazy {
        SecureKeyStore(appContext)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    override val audioRoutingManager: AudioRoutingManager by lazy {
        AudioRoutingManager(appContext)
    }

    override val sessionRegistry: AudioSessionRegistry by lazy {
        AudioSessionRegistry(appContext, audioRoutingManager)
    }

    override val eqStateStore: EqStateStore by lazy {
        EqStateStore(sessionRegistry)
    }

    private val database: SmartEqDatabase by lazy {
        SmartEqDatabase.build(appContext)
    }

    override val presetRepository: PresetRepository by lazy {
        PresetRepository(database.presetDao())
    }

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(GeminiModel.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)
    }

    private val spotifyApi: SpotifyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpotifyApi::class.java)
    }

    override val geminiClient: GeminiClient by lazy {
        GeminiClient(geminiApi, json)
    }

    private val spotifyAuthManager: SpotifyAuthManager by lazy {
        SpotifyAuthManager(appContext, secureKeyStore, okHttpClient, json)
    }

    override val spotifyRepository: SpotifyRepository by lazy {
        SpotifyRepository(spotifyApi, spotifyAuthManager, secureKeyStore)
    }

    override val artistTuningRepository: ArtistTuningRepository by lazy {
        ArtistTuningRepository(geminiClient, secureKeyStore)
    }

    override val appLockManager: AppLockManager by lazy {
        AppLockManager(secureKeyStore, settingsRepository)
    }

    override val localMediaMetadataReader: LocalMediaMetadataReader by lazy {
        LocalMediaMetadataReader(appContext)
    }
}
