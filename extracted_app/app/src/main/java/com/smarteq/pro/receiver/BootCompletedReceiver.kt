package com.smarteq.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.service.EqualizerForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Only takes action if the user explicitly turned on
 * Settings > "Resume equalizer on boot" — off by default. Android 15+
 * restricts starting most foreground service types directly from a
 * BOOT_COMPLETED receiver, but specialUse (and this pattern of checking
 * a saved preference first) is the documented-compliant approach; the
 * actual startForegroundService() call happens promptly within this
 * receiver's onReceive, matching platform guidance for boot-time starts.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
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
