package com.nana.media

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import com.nana.common.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive media state provider for UI.
 */
class MediaStateProvider(private val context: Context) {
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val _nowPlaying = MutableStateFlow<NowPlayingInfo?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    private var activeController: MediaController? = null

    fun start() {
        updateActiveSession()
    }

    fun stop() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    private fun updateActiveSession() {
        val controllers = mediaSessionManager.activeSessions
        controllers?.forEach { controller ->
            if (controller.playbackState?.state == MediaController.PlaybackState.STATE_PLAYING) {
                setController(controller)
                return
            }
        }
        controllers?.firstOrNull()?.let { setController(it) }
    }

    private fun setController(controller: MediaController) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        controller.registerCallback(controllerCallback)
        updateNowPlaying()
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaController.Metadata?) {
            updateNowPlaying()
        }
        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
            updateNowPlaying()
        }
    }

    private fun updateNowPlaying() {
        val metadata = activeController?.metadata
        val state = activeController?.playbackState
        _nowPlaying.value = if (metadata != null) {
            NowPlayingInfo(
                title = metadata.description.title?.toString() ?: "Unknown",
                artist = metadata.description.subtitle?.toString() ?: "Unknown",
                album = metadata.description.description?.toString(),
                artworkUri = metadata.description.iconUri,
                packageName = activeController?.packageName,
                isPlaying = state?.state == android.media.session.PlaybackState.STATE_PLAYING,
                duration = metadata.duration,
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
