package com.smarteq.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import com.smarteq.pro.audio.engine.AudioSessionRegistry
import com.smarteq.pro.service.EqualizerForegroundService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Listens for AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION and
 * ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION — the official, documented
 * broadcasts a well-behaved media/music app sends when it starts/stops an
 * audio session that should accept external effects. This is "Default
 * mode." See android/media/audiofx/AudioEffect docs for the exact extras
 * used here (EXTRA_AUDIO_SESSION, EXTRA_PACKAGE_NAME).
 *
 * Known real-world limitation, disclosed to the user rather than hidden:
 * not every app broadcasts these reliably (documented cases include some
 * OEM music apps and, per public reports, some third-party streaming
 * apps under certain conditions) — Legacy mode exists as a fallback for
 * exactly that situation.
 */
@AndroidEntryPoint
class AudioSessionReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRegistry: AudioSessionRegistry

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        if (sessionId <= 0) return
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)

        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.i(TAG, "Session opened: id=$sessionId pkg=$packageName")
                sessionRegistry.onSessionOpened(sessionId, packageName)
                // Ensure the engine service is running so the notification
                // reflects reality and Android doesn't reclaim our process.
                EqualizerForegroundService.ensureStarted(context)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.i(TAG, "Session closed: id=$sessionId pkg=$packageName")
                sessionRegistry.onSessionClosed(sessionId)
            }
        }
    }

    private companion object {
        const val TAG = "AudioSessionReceiver"
    }
}
