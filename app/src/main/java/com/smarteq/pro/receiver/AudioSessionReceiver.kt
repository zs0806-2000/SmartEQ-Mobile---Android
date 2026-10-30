package com.smarteq.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import com.smarteq.pro.SmartEqApplication
import com.smarteq.pro.service.EqualizerForegroundService

/**
 * Listens for AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION and
 * ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION from media/music players.
 */
class AudioSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        if (sessionId <= 0) return
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)

        val app = context.applicationContext as? SmartEqApplication ?: return
        val sessionRegistry = app.container.sessionRegistry

        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.i(TAG, "Session opened: id=$sessionId pkg=$packageName")
                sessionRegistry.onSessionOpened(sessionId, packageName)
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
