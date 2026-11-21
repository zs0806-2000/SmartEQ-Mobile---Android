package com.smarteq.pro.audio.adaptive

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What is playing right now, as reported by the playing app's media session. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String?,
    val packageName: String,
    val isPlaying: Boolean,
) {
    /** Identity used to detect a track change (same song resumed is not a new track). */
    val trackKey: String get() = "$packageName|$artist|$title"
}

/**
 * Holds the current track. Filled only by [com.smarteq.pro.service.NowPlayingListenerService],
 * which runs only if the user grants notification access. Never stored or sent anywhere.
 */
class NowPlayingRepository {
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    private val _listenerConnected = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = _listenerConnected

    fun update(value: NowPlaying?) {
        _nowPlaying.value = value
    }

    fun setListenerConnected(connected: Boolean) {
        _listenerConnected.value = connected
        if (!connected) _nowPlaying.value = null
    }
}
