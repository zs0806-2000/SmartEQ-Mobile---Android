package com.smarteq.pro.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log
import com.smarteq.pro.SmartEqApplication
import com.smarteq.pro.audio.adaptive.NowPlaying

/**
 * Reads only "what's playing" (title, artist, album, which app) from active media sessions,
 * so adaptive tuning knows when a new track starts. This works for any player, including
 * Spotify, YouTube Music and Tidal, with no account login.
 *
 * Android only lets an app see other apps' media sessions if the user grants it
 * notification access, which is why this is a NotificationListenerService. It deliberately
 * does NOT read, store or react to the content of any notification: onNotificationPosted
 * is not overridden.
 */
class NowPlayingListenerService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private var sessionManager: MediaSessionManager? = null
    private val controllers = mutableMapOf<TokenKey, Pair<MediaController, MediaController.Callback>>()

    private val repo get() = (applicationContext as SmartEqApplication).container.nowPlayingRepository

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        rebind(list ?: emptyList())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        repo.setListenerConnected(true)
        val sm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        sessionManager = sm
        val me = ComponentName(this, NowPlayingListenerService::class.java)
        runCatching {
            sm.addOnActiveSessionsChangedListener(sessionsListener, me, handler)
            rebind(sm.getActiveSessions(me))
        }.onFailure { Log.w(TAG, "Media sessions unavailable", it) }
    }

    override fun onListenerDisconnected() {
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        unbindAll()
        repo.setListenerConnected(false)
        super.onListenerDisconnected()
    }

    private fun rebind(list: List<MediaController>) {
        unbindAll()
        list.forEach { controller ->
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onSessionDestroyed() = publish()
            }
            controller.registerCallback(cb, handler)
            controllers[TokenKey(controller.sessionToken.hashCode(), controller.packageName)] = controller to cb
        }
        publish()
    }

    private fun unbindAll() {
        controllers.values.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
        controllers.clear()
    }

    /** Publishes the session that is actually playing (or, if none, the first one with metadata). */
    private fun publish() {
        val all = controllers.values.map { it.first }
        val playing = all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val chosen = playing ?: all.firstOrNull { it.metadata != null }
        val md = chosen?.metadata
        if (chosen == null || md == null) {
            repo.update(null)
            return
        }
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
        repo.update(
            NowPlaying(
                title = title,
                artist = artist,
                album = md.getString(MediaMetadata.METADATA_KEY_ALBUM),
                packageName = chosen.packageName,
                isPlaying = playing != null,
            )
        )
    }

    private data class TokenKey(val token: Int, val pkg: String)

    companion object {
        private const val TAG = "NowPlayingListener"

        /** True if the user has granted SmartyQ notification access. */
        fun isAccessGranted(context: Context): Boolean {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return false
            return nm.isNotificationListenerAccessGranted(ComponentName(context, NowPlayingListenerService::class.java))
        }
    }
}
