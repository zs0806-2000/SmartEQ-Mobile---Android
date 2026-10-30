package com.smarteq.pro.audio.engine

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.smarteq.pro.data.model.ActiveAudioSession
import com.smarteq.pro.data.model.AudioOutputDevice
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.EqEngineMode
import com.smarteq.pro.data.model.EqEngineStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the live set of DSP engine instances: session 0 for Universal Global audio,
 * or per-app sessions. Tracks what apps are playing and what output device is receiving audio.
 */
class AudioSessionRegistry(
    private val appContext: Context,
    private val audioRoutingManager: AudioRoutingManager,
) {

    private val engines = mutableMapOf<Int, SmartEqDspEngine>()
    private val sessionOwners = mutableMapOf<Int, String>() // sessionId -> package name

    private var currentBandCount = 15
    private var currentGains: List<Float> = List(15) { 0f }
    private var currentDynamics: DynamicsSettings = DynamicsSettings.DEFAULT
    private var currentMode: EqEngineMode = EqEngineMode.UNIVERSAL_GLOBAL
    private var isLowLatencyActive: Boolean = false
    private var currentOutputDevice: AudioOutputDevice = AudioOutputDevice()

    private val _status = MutableStateFlow(EqEngineStatus())
    val status: StateFlow<EqEngineStatus> = _status

    init {
        audioRoutingManager.setOnRoutingChangedListener { device, lowLatency ->
            onRoutingUpdated(device, lowLatency)
        }
    }

    @Synchronized
    private fun onRoutingUpdated(device: AudioOutputDevice, lowLatency: Boolean) {
        val routingChanged = isLowLatencyActive != lowLatency || currentOutputDevice != device
        currentOutputDevice = device
        isLowLatencyActive = lowLatency

        if (routingChanged && engines.isNotEmpty()) {
            engines.values.forEach { engine ->
                engine.apply(currentGains, currentDynamics, currentBandCount, lowLatency)
            }
        }
        publishStatus()
    }

    @Synchronized
    fun setMode(mode: EqEngineMode) {
        if (mode == currentMode && engines.isNotEmpty()) return
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
            sessionOwners[sessionId] = pkg
            // Also ensure global session 0 is active
            if (!engines.containsKey(0)) {
                openSession(sessionId = 0, packageName = appContext.packageName)
            }
            publishStatus()
            return
        }
        if (currentMode != EqEngineMode.DEFAULT_PER_SESSION) return
        if (sessionId == 0) return
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
        if (engine.attach(currentBandCount, lowLatency = isLowLatencyActive)) {
            engine.apply(currentGains, currentDynamics, currentBandCount, isLowLatencyActive)
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
        engines.values.forEach { it.apply(bandGainsDb, dynamics, bandCount, isLowLatencyActive) }
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
        _status.update {
            it.copy(
                isRunning = engines.isNotEmpty(),
                mode = currentMode,
                activeSessions = sessions,
                currentOutputDevice = currentOutputDevice,
                isLowLatencyActive = isLowLatencyActive
            )
        }
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

    companion object {
        private const val TAG = "AudioSessionRegistry"
    }
}
