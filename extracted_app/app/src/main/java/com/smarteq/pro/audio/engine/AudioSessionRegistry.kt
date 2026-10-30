package com.smarteq.pro.audio.engine

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.smarteq.pro.data.model.ActiveAudioSession
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.EqEngineMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the live set of DSP engine instances: one per currently-open audio
 * session in Default mode, or a single instance bound to session 0 in
 * Legacy mode. This is the class the foreground service and the receiver
 * both talk to; it is the single source of truth for "what is currently
 * being equalized," which the Equalizer screen displays honestly (see
 * EqEngineStatus / help_system_wide_body).
 */
@Singleton
class AudioSessionRegistry @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    private val engines = mutableMapOf<Int, SmartEqDspEngine>()
    private val sessionOwners = mutableMapOf<Int, String>() // sessionId -> package name

    private var currentBandCount = 15
    private var currentGains: List<Float> = List(15) { 0f }
    private var currentDynamics: DynamicsSettings = DynamicsSettings.DEFAULT
    private var currentMode: EqEngineMode = EqEngineMode.UNIVERSAL_GLOBAL

    private val _status = MutableStateFlow(com.smarteq.pro.data.model.EqEngineStatus())
    val status: StateFlow<com.smarteq.pro.data.model.EqEngineStatus> = _status

    @Synchronized
    fun setMode(mode: EqEngineMode) {
        if (mode == currentMode && engines.isNotEmpty()) return
        // Clean rebuild on mode change
        releaseAll()
        currentMode = mode
        if (mode == EqEngineMode.UNIVERSAL_GLOBAL || mode == EqEngineMode.LEGACY_GLOBAL) {
            openSession(sessionId = 0, packageName = appContext.packageName)
        }
        publishStatus()
    }

    @Synchronized
    fun onSessionOpened(sessionId: Int, packageName: String?) {
        val pkg = packageName ?: "Media Playback"
        if (currentMode == EqEngineMode.UNIVERSAL_GLOBAL) {
            // In Universal mode, session 0 handles the audio DSP globally for all apps,
            // while we record active player sessions so the UI and notifications reflect what's playing.
            sessionOwners[sessionId] = pkg
            publishStatus()
            return
        }
        if (currentMode != EqEngineMode.DEFAULT_PER_SESSION) return
        if (sessionId == 0) return // session 0 is only meaningful in Global/Legacy modes
        openSession(sessionId, pkg)
    }

    @Synchronized
    fun onSessionClosed(sessionId: Int) {
        if (currentMode == EqEngineMode.UNIVERSAL_GLOBAL) {
            sessionOwners.remove(sessionId)
            publishStatus()
            return
        }
        if (currentMode != EqEngineMode.DEFAULT_PER_SESSION) return
        engines.remove(sessionId)?.release()
        sessionOwners.remove(sessionId)
        publishStatus()
    }

    private fun openSession(sessionId: Int, packageName: String) {
        if (engines.containsKey(sessionId)) return
        val engine = SmartEqDspEngine(sessionId)
        if (engine.attach(currentBandCount)) {
            engine.apply(currentGains, currentDynamics, currentBandCount)
            engines[sessionId] = engine
            sessionOwners[sessionId] = packageName
            publishStatus()
        }
    }

    /** Broadcasts a new EQ configuration to every currently-attached session at once. */
    @Synchronized
    fun updateConfiguration(bandGainsDb: List<Float>, dynamics: DynamicsSettings, bandCount: Int) {
        currentGains = bandGainsDb
        currentDynamics = dynamics
        currentBandCount = bandCount
        engines.values.forEach { it.apply(bandGainsDb, dynamics, bandCount) }
    }

    @Synchronized
    fun setOutputTrim(trimDb: Float) {
        engines.values.forEach { it.setOutputTrim(trimDb) }
    }

    @Synchronized
    fun releaseAll() {
        engines.values.forEach { it.release() }
        engines.clear()
        sessionOwners.clear()
        publishStatus()
    }

    private fun publishStatus() {
        val sessions = sessionOwners.map { (sessionId, pkg) ->
            ActiveAudioSession(sessionId, pkg, resolveAppLabel(pkg))
        }
        _status.update { it.copy(isRunning = engines.isNotEmpty(), mode = currentMode, activeSessions = sessions) }
    }

    private fun resolveAppLabel(packageName: String): String {
        return runCatching {
            val pm = appContext.packageManager
            val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    private companion object {
        const val TAG = "AudioSessionRegistry"
    }
}
