package com.smarteq.pro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smarteq.pro.MainActivity
import com.smarteq.pro.R
import com.smarteq.pro.SmartEqApplication
import com.smarteq.pro.audio.engine.AudioSessionRegistry
import com.smarteq.pro.data.model.AudioOutputDevice
import com.smarteq.pro.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Always-on foreground service hosting the real-time system EQ engine.
 * Extends standard android.app.Service with FOREGROUND_SERVICE_TYPE_SPECIAL_USE.
 */
class EqualizerForegroundService : Service() {

    private lateinit var sessionRegistry: AudioSessionRegistry
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as SmartEqApplication
        sessionRegistry = app.container.sessionRegistry
        settingsRepository = app.container.settingsRepository
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification channel MUST be created before startForeground on Android 8+
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(processing = false, label = null, device = null),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(processing = false, label = null, device = null))
        }

        settingsRepository.eqEngineMode
            .onEach { mode -> sessionRegistry.setMode(mode) }
            .launchIn(serviceScope)

        settingsRepository.eqEngineEnabled
            .onEach { enabled -> if (!enabled) sessionRegistry.releaseAll() }
            .launchIn(serviceScope)

        sessionRegistry.status
            .onEach { status ->
                val activeLabel = status.activeSessions.firstOrNull()?.sourceAppLabel
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(status.isRunning, activeLabel, status.currentOutputDevice)
                )
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        sessionRegistry.releaseAll()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopSelfCleanly() {
        sessionRegistry.releaseAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(processing: Boolean, label: String?, device: AudioOutputDevice?): Notification {
        val deviceSuffix = if (device != null && device.name.isNotBlank()) " • ${device.name}" else ""
        val contentText = if (processing && label != null) {
            "${getString(R.string.fgs_notification_text_processing, label)}$deviceSuffix"
        } else if (processing) {
            "Equalizing system audio$deviceSuffix"
        } else {
            getString(R.string.fgs_notification_text_idle)
        }

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, EqualizerForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_fader)
            .setContentTitle(getString(R.string.fgs_notification_title_active))
            .setContentText(contentText)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.fgs_notification_action_disable), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.fgs_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.fgs_notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "smarteq_engine_status"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_STOP = "com.smarteq.pro.action.STOP_ENGINE"

        fun ensureStarted(context: Context) {
            val intent = Intent(context, EqualizerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, EqualizerForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
