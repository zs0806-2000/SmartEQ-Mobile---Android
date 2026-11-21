package com.smarteq.pro.audio.engine

import android.content.Context
import android.content.pm.PackageManager
import com.smarteq.pro.data.model.ActiveAudioSession
import com.smarteq.pro.data.model.AudioOutputDevice
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.EqEngineMode
import com.smarteq.pro.data.model.EqEngineStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the live DSP engine instances.
 *
 * Modes:
 *  - UNIVERSAL_GLOBAL ("Automatic"): attach to each app's own audio session as soon as the app
 *    announces it. Only while no app has announced a session does it fall back to the phone's
 *    global output mix (session 0). Both are never active at once, because processing the same
 *    audio twice (per-app EQ, then the global EQ on top) doubles every boost and cut.
 *  - DEFAULT_PER_SESSION: per-app sessions only.
 *  - LEGACY_GLOBAL: global output mix only. Deprecated by Android and unsupported on some phones.
 */
class AudioSessionRegistry(
    private val appContext: Context,
    private val audioRoutingManager: AudioRoutingManager,
) {
    private val engines = mutableMapOf<Int, SmartEqDspEngine>()
    private val sessionOwners = linkedMapOf<Int, String>() // sessionId -> package, in open order

    private var currentGains15: List<Float> = List(EqCurveMath.CENTERS_15.size) { 0f }
    private var currentDynamics: DynamicsSettings = DynamicsSettings.DEFAULT
    private var currentInputGainDb: Float = 0f
    private var currentMode: EqEngineMode = EqEngineMode.UNIVERSAL_GLOBAL
    private var isLowLatencyActive: Boolean = false
    private var currentOutputDevice: AudioOutputDevice = AudioOutputDevice()
    private var active = false

    private val _status = MutableStateFlow(EqEngineStatus())
    val status: StateFlow<EqEngineStatus> = _status

    init {
        audioRoutingManager.setOnRoutingChangedListener { device, lowLatency ->
            onRoutingUpdated(device, lowLatency)
        }
    }

    @Synchronized
    private fun onRoutingUpdated(device: AudioOutputDevice, lowLatency: Boolean) {
        val changed = isLowLatencyActive != lowLatency || currentOutputDevice != device
        currentOutputDevice = device
        isLowLatencyActive = lowLatency
        if (changed) applyAll()
        publishStatus()
    }

    /** Starts processing in [mode]. Called by the foreground service. */
    @Synchronized
    fun setMode(mode: EqEngineMode) {
        if (active && mode == currentMode) return
        releaseEnginesOnly()
        currentMode = mode
        active = true
        // Re-attach to any app sessions we already know about.
        if (mode != EqEngineMode.LEGACY_GLOBAL) {
            sessionOwners.keys.toList().forEach { attachEngine(it) }
        }
        reconcileGlobalSession()
        publishStatus()
    }

    @Synchronized
    fun onSessionOpened(sessionId: Int, packageName: String?) {
        if (sessionId <= 0) return
        sessionOwners.remove(sessionId)
        sessionOwners[sessionId] = packageName ?: "unknown"
        if (active && currentMode != EqEngineMode.LEGACY_GLOBAL) attachEngine(sessionId)
        reconcileGlobalSession()
        publishStatus()
    }

    @Synchronized
    fun onSessionClosed(sessionId: Int) {
        sessionOwners.remove(sessionId)
        engines.remove(sessionId)?.release()
        reconcileGlobalSession()
        publishStatus()
    }

    /** The session the adaptive analyzer should listen to: the playing app's, else the global mix. */
    @Synchronized
    fun analysisSessionFor(packageName: String?): Int {
        if (packageName != null) {
            sessionOwners.entries.lastOrNull { it.value == packageName }?.let { return it.key }
        }
        return sessionOwners.keys.lastOrNull() ?: 0
    }

    @Synchronized
    fun updateConfiguration(gains15: List<Float>, dynamics: DynamicsSettings, inputGainDb: Float) {
        currentGains15 = gains15
        currentDynamics = dynamics
        currentInputGainDb = inputGainDb
        applyAll()
    }

    /** Stops all processing (engine switched off). Known app sessions are remembered. */
    @Synchronized
    fun releaseAll() {
        releaseEnginesOnly()
        active = false
        publishStatus()
    }

    private fun releaseEnginesOnly() {
        engines.values.forEach { it.release() }
        engines.clear()
    }

    private fun reconcileGlobalSession() {
        if (!active) return
        val wantGlobal = when (currentMode) {
            EqEngineMode.LEGACY_GLOBAL -> true
            EqEngineMode.DEFAULT_PER_SESSION -> false
            EqEngineMode.UNIVERSAL_GLOBAL -> engines.keys.none { it != 0 }
        }
        if (wantGlobal && !engines.containsKey(0)) attachEngine(0)
        if (!wantGlobal) engines.remove(0)?.release()
    }

    private fun attachEngine(sessionId: Int) {
        if (engines.containsKey(sessionId)) return
        val engine = SmartEqDspEngine(sessionId)
        if (engine.attach(lowLatency = isLowLatencyActive)) {
            engine.apply(currentGains15, currentDynamics, currentInputGainDb, isLowLatencyActive)
            engines[sessionId] = engine
        }
    }

    private fun applyAll() {
        engines.values.forEach { it.apply(currentGains15, currentDynamics, currentInputGainDb, isLowLatencyActive) }
    }

    private fun publishStatus() {
        val shown = if (engines.containsKey(0) && engines.size == 1) {
            listOf(ActiveAudioSession(0, appContext.packageName, "All system audio"))
        } else {
            engines.keys.filter { it != 0 }.map { id ->
                val pkg = sessionOwners[id] ?: "unknown"
                ActiveAudioSession(id, pkg, resolveAppLabel(pkg))
            }
        }
        _status.update {
            it.copy(
                isRunning = engines.isNotEmpty(),
                mode = currentMode,
                activeSessions = shown,
                currentOutputDevice = currentOutputDevice,
                isLowLatencyActive = isLowLatencyActive,
                usingFallbackEqualizer = engines.values.any { e -> e.isFallback },
            )
        }
    }

    private fun resolveAppLabel(packageName: String): String = runCatching {
        val pm = appContext.packageManager
        val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        pm.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)
}
