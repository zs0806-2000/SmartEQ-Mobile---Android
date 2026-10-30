package com.smarteq.pro.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.util.Log
import com.smarteq.pro.data.model.TrackMetadata
import com.smarteq.pro.data.model.TrackSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Listens for system-wide active media sessions (Spotify, YouTube Music,
 * Apple Music, VLC, local file players, etc.) to detect real-time
 * artist, title, and album metadata for automatic Auto-Tuning.
 */
class MediaSessionListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaSessionListener"

        private val _currentTrack = MutableStateFlow<TrackMetadata?>(null)
        val currentTrack: StateFlow<TrackMetadata?> = _currentTrack.asStateFlow()

        fun updateTrack(title: String, artist: String, album: String? = null) {
            _currentTrack.value = TrackMetadata(
                title = title,
                artist = artist,
                album = album,
                source = TrackSource.SYSTEM_SESSION,
            )
        }
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private val activeControllers = mutableListOf<MediaController>()

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.let { registerControllerCallbacks(it) }
    }

    override fun onCreate() {
        super.onCreate()
        initMediaSessionManager()
    }

    private fun initMediaSessionManager() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(this, MediaSessionListenerService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, componentName)

            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            controllers?.let { registerControllerCallbacks(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register MediaSessionManager listener", e)
        }
    }

    private fun registerControllerCallbacks(controllers: List<MediaController>) {
        activeControllers.clear()
        for (controller in controllers) {
            activeControllers.add(controller)
            extractMetadata(controller)

            controller.registerCallback(object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    extractMetadata(controller)
                }
            })
        }
    }

    private fun extractMetadata(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Active Track"
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)

        Log.d(TAG, "Real-time track update from [${controller.packageName}]: $artist - $title")
        updateTrack(title, artist, album)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering session listener", e)
        }
    }
}
