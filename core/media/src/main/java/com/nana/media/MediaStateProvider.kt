package com.nana.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive media state provider for UI.
 */
class MediaStateProvider(private val context: Context) {
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val _nowPlaying = MutableStateFlow<NowPlayingInfo?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateNowPlaying()
        override fun onPlaybackStateChanged(state: PlaybackState?) = updateNowPlaying()
        override fun onSessionDestroyed() = updateNowPlaying()
    }

    fun start() {
        updateActiveSession()
    }

    fun stop() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    private fun updateActiveSession() {
        val controllers = try {
            mediaSessionManager.getActiveSessions(null as ComponentName?)
        } catch (e: Exception) {
            emptyList()
        }
        controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?.let { setController(it); return }
        controllers.firstOrNull()?.let { setController(it) }
    }

    private fun setController(controller: MediaController) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        controller.registerCallback(controllerCallback)
        updateNowPlaying()
    }

    private fun updateNowPlaying() {
        val metadata = activeController?.metadata
        val state = activeController?.playbackState
        _nowPlaying.value = if (metadata != null) {
            NowPlayingInfo(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
                artworkUri = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)?.let(android.net.Uri::parse),
                packageName = activeController?.packageName,
                isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
                position = state?.position ?: 0
            )
        } else null
    }

    data class NowPlayingInfo(
        val title: String,
        val artist: String,
        val album: String?,
        val artworkUri: android.net.Uri?,
        val packageName: String?,
        val isPlaying: Boolean,
        val duration: Long,
        val position: Long
    )
}
