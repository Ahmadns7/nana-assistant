package com.nana.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Media control and metadata using the Android media session framework.
 * Provides accurate now-playing info and transport controls.
 */
class MediaController(private val context: Context) {
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null
    private var controllerCallback: MediaControllerCallback? = null

    private val frameworkCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            controllerCallback?.onMetadataChanged()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            controllerCallback?.onPlaybackStateChanged(state?.state ?: PlaybackState.STATE_NONE)
        }

        override fun onSessionDestroyed() {
            controllerCallback?.onSessionChanged(null)
        }
    }

    interface MediaControllerCallback {
        fun onMetadataChanged()
        fun onPlaybackStateChanged(state: Int)
        fun onSessionChanged(packageName: String?)
    }

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val controllers = try {
                mediaSessionManager.getActiveSessions(null as ComponentName?)
            } catch (e: Exception) {
                emptyList()
            }
            selectBestController(controllers)
            NanaLogger.i("MediaController", "Initialized, active session: ${activeController?.packageName}")
            Result.Success(Unit)
        } catch (e: Exception) {
            NanaLogger.e("MediaController", "Init failed", e)
            Result.Failure(Result.Error("INIT_FAILED", "Media controller init failed", e))
        }
    }

    private fun selectBestController(controllers: List<MediaController>) {
        controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?.let { setActiveController(it); return }
        controllers.firstOrNull()?.let { setActiveController(it) }
    }

    private fun setActiveController(controller: MediaController) {
        activeController?.unregisterCallback(frameworkCallback)
        activeController = controller
        controller.registerCallback(frameworkCallback)
        controllerCallback?.onSessionChanged(controller.packageName)
    }

    fun setCallback(callback: MediaControllerCallback) {
        controllerCallback = callback
    }

    fun getCurrentMetadata(): MediaMetadata? = activeController?.metadata

    fun getPlaybackState(): Int = activeController?.playbackState?.state ?: PlaybackState.STATE_NONE

    fun isPlaying(): Boolean = getPlaybackState() == PlaybackState.STATE_PLAYING

    fun getActivePackageName(): String? = activeController?.packageName

    private fun transport(action: (MediaController.TransportControls) -> Unit): Result<Unit> {
        val controller = activeController
            ?: return Result.Failure(Result.Error("NO_SESSION", "No active media session"))
        action(controller.transportControls)
        return Result.Success(Unit)
    }

    suspend fun play(): Result<Unit> = withContext(Dispatchers.IO) { transport { it.play() } }

    suspend fun pause(): Result<Unit> = withContext(Dispatchers.IO) { transport { it.pause() } }

    suspend fun next(): Result<Unit> = withContext(Dispatchers.IO) { transport { it.skipToNext() } }

    suspend fun previous(): Result<Unit> = withContext(Dispatchers.IO) { transport { it.skipToPrevious() } }

    suspend fun setVolume(volume: Float): Result<Unit> = withContext(Dispatchers.IO) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val targetVolume = (maxVolume * volume.coerceIn(0f, 1f)).toInt()
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVolume, 0)
        Result.Success(Unit)
    }

    suspend fun adjustVolume(direction: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, 0)
        Result.Success(Unit)
    }

    fun getVolume(): Float {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
    }

    fun getNowPlayingInfo(): NowPlayingInfo? {
        val metadata = getCurrentMetadata() ?: return null
        val state = getPlaybackState()
        return NowPlayingInfo(
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
            artworkUri = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)?.let(android.net.Uri::parse),
            packageName = activeController?.packageName,
            isPlaying = state == PlaybackState.STATE_PLAYING,
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
            position = activeController?.playbackState?.position ?: 0
        )
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
    ) {
        fun toSpokenString(): String {
            return if (isPlaying) {
                "Now playing $title by $artist${packageName?.let { " from $it" } ?: ""}"
            } else {
                "Paused: $title by $artist"
            }
        }
    }

    fun release() {
        activeController?.unregisterCallback(frameworkCallback)
        activeController = null
    }
}
