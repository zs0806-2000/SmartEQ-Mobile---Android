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
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.smarteq.pro.MainActivity
import com.smarteq.pro.R
import com.smarteq.pro.audio.engine.AudioSessionRegistry
import com.smarteq.pro.data.model.EqEngineMode
import com.smarteq.pro.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Always-on (while enabled) foreground service hosting the real-time EQ
 * engine. Uses FOREGROUND_SERVICE_TYPE_SPECIAL_USE with a declared
 * PROPERTY_SPECIAL_USE_FGS_SUBTYPE (see AndroidManifest.xml) since
 * "apply real-time audio effects to other apps' playback" doesn't fit
 * mediaPlayback (this service does not play media) or mediaProcessing
 * (that type is capped at 6 hours per day and meant for file transcoding,
 * not indefinite real-time processing).
 *
 * Uses LifecycleService (not a plain Service) purely so we can collect
 * Kotlin Flows with lifecycleScope without hand-rolling a CoroutineScope.
 */
@AndroidEntryPoint
class EqualizerForegroundService : LifecycleService() {

    @Inject lateinit var sessionRegistry: AudioSessionRegistry
    @Inject lateinit var settingsRepository: SettingsRepository

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(processing = false, label = null),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(processing = false, label = null))
        }

        settingsRepository.eqEngineMode
            .onEach { mode -> sessionRegistry.setMode(mode) }
            .launchIn(lifecycleScope)

        settingsRepository.eqEngineEnabled
            .onEach { enabled -> if (!enabled) sessionRegistry.releaseAll() }
            .launchIn(lifecycleScope)

        sessionRegistry.status
            .onEach { status ->
                val activeLabel = status.activeSessions.firstOrNull()?.sourceAppLabel
                notificationManager.notify(NOTIFICATION_ID, buildNotification(status.isRunning, activeLabel))
            }
            .launchIn(lifecycleScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null // Not a bound service — all communication is via the shared Hilt singletons.
    }

    override fun onDestroy() {
        sessionRegistry.releaseAll()
        super.onDestroy()
    }

    private fun stopSelfCleanly() {
        sessionRegistry.releaseAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(processing: Boolean, label: String?): Notification {
        val contentText = if (processing && label != null) {
            getString(R.string.fgs_notification_text_processing, label)
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
