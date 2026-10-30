package com.smarteq.pro.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.smarteq.pro.data.model.TrackMetadata
import com.smarteq.pro.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads title/artist/album from a locally-selected audio file via MediaMetadataRetriever.
 */
class LocalMediaMetadataReader(
    private val context: Context,
) {
    suspend fun read(uri: Uri, displayName: String): TrackMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            if (!title.isNullOrBlank() || !artist.isNullOrBlank()) {
                return@withContext TrackMetadata(
                    title = title?.trim().takeUnless { it.isNullOrBlank() } ?: parseFileName(displayName).first,
                    artist = artist?.trim().takeUnless { it.isNullOrBlank() } ?: parseFileName(displayName).second,
                    album = album?.trim(),
                    source = TrackSource.LOCAL_FILE,
                )
            }
        } catch (e: Exception) {
            // Fall through to filename parsing — never a hard failure.
        } finally {
            runCatching { retriever.release() }
        }

        val (fallbackTitle, fallbackArtist) = parseFileName(displayName)
        TrackMetadata(title = fallbackTitle, artist = fallbackArtist, source = TrackSource.LOCAL_FILE)
    }

    private fun parseFileName(fileName: String): Pair<String, String> {
        val withoutExt = fileName.substringBeforeLast('.', fileName)
        val cleaned = withoutExt.replace(Regex("^[0-9]{1,3}[\\s._-]+"), "")
        val parts = cleaned.split(Regex("\\s+[-\u2013\u2014]\\s+"))
        return if (parts.size >= 2) {
            val artist = parts[0].trim()
            val title = parts.drop(1).joinToString(" - ").trim()
            title to artist
        } else {
            val title = cleaned.trim().ifBlank { fileName }
            title to "Unknown Artist"
        }
    }
}
