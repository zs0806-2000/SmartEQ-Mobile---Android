package com.smarteq.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smarteq.pro.SmartEqApplication
import com.smarteq.pro.service.EqualizerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restarts equalizer foreground service on boot if user enabled it in Settings.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? SmartEqApplication ?: return
        val settingsRepository = app.container.settingsRepository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val shouldResume = settingsRepository.resumeOnBoot.first()
                val engineEnabled = settingsRepository.eqEngineEnabled.first()
                if (shouldResume && engineEnabled) {
                    EqualizerForegroundService.ensureStarted(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
