package com.nana.media

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.media3.session.MediaController
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommandGroup
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Media control and metadata via Media3 MediaController.
 * Provides accurate now-playing info and transport controls.
 */
class MediaController(private val context: Context) {
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null
    private var controllerCallback: MediaControllerCallback? = null

    interface MediaControllerCallback {
        fun onMetadataChanged(metadata: MediaMetadata?)
        fun onPlaybackStateChanged(state: Int)
        fun onSessionChanged(packageName: String?)
    }

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Find active media session
            val controllers = mediaSessionManager.activeSessions
            selectBestController(controllers)
            NanaLogger.i("MediaController", "Initialized, active session: ${activeController?.packageName}")
            Result.Success(Unit)
        } catch (e: Exception) {
            NanaLogger.e("MediaController", "Init failed", e)
            Result.Failure(Result.Error("INIT_FAILED", "Media controller init failed", e))
        }
    }

    private fun selectBestController(controllers: java.util.List<MediaController>?) {
        controllers?.forEach { controller ->
            val state = controller.playbackState
            if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                setActiveController(controller)
                return
            }
        }
        // Fallback to first available
        controllers?.firstOrNull()?.let { setActiveController(it) }
    }

    private fun setActiveController(controller: MediaController) {
        activeController?.unregisterCallback(controllerCallback!!)
        activeController = controller
        controllerCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                controllerCallback?.onMetadataChanged(metadata)
            }
            override fun onPlaybackStateChanged(state: Int) {
                controllerCallback?.onPlaybackStateChanged(state)
            }
        }
        controller.registerCallback(controllerCallback!!)
        controllerCallback?.onSessionChanged(controller.packageName)
    }

    fun setCallback(callback: MediaControllerCallback) {
        controllerCallback = callback
    }

    fun getCurrentMetadata(): MediaMetadata? = activeController?.getMediaMetadata()

    fun getPlaybackState(): Int = activeController?.playbackState?.state ?: PlaybackState.STATE_NONE

    fun isPlaying(): Boolean = getPlaybackState() == PlaybackState.STATE_PLAYING

    fun getActivePackageName(): String? = activeController?.packageName

    suspend fun play(): Result<Unit> = withContext(Dispatchers.IO) {
        activeController?.getSessionController()?.sendSessionCommand(SessionCommand(SessionCommand.COMMAND_CODE_PLAY))
            ?: return Result.Failure(Result.Error("NO_SESSION", "No active media session"))
        Result.Success(Unit)
    }

    suspend fun pause(): Result<Unit> = withContext(Dispatchers.IO) {
        activeController?.getSessionController()?.sendSessionCommand(SessionCommand(SessionCommand.COMMAND_CODE_PAUSE))
            ?: return Result.Failure(Result.Error("NO_SESSION", "No active media session"))
        Result.Success(Unit)
    }

    suspend fun next(): Result<Unit> = withContext(Dispatchers.IO) {
        activeController?.getSessionController()?.sendSessionCommand(SessionCommand(SessionCommand.COMMAND_CODE_SKIP_TO_NEXT))
            ?: return Result.Failure(Result.Error("NO_SESSION", "No active media session"))
        Result.Success(Unit)
    }

    suspend fun previous(): Result<Unit> = withContext(Dispatchers.IO) {
        activeController?.getSessionController()?.sendSessionCommand(SessionCommand(SessionCommand.COMMAND_CODE_SKIP_TO_PREVIOUS))
            ?: return Result.Failure(Result.Error("NO_SESSION", "No active media session"))
        Result.Success(Unit)
    }

    suspend fun setVolume(volume: Float): Result<Unit> = withContext(Dispatchers.IO) {
        // Volume control via system audio manager
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
        val metadata = getCurrentMetadata()
        val state = getPlaybackState()
        return if (metadata != null) {
            NowPlayingInfo(
                title = metadata.title?.toString() ?: "Unknown",
                artist = metadata.artist?.toString() ?: "Unknown",
                album = metadata.album?.toString(),
                artworkUri = metadata.artworkUri,
                packageName = activeController?.packageName,
                isPlaying = state == PlaybackState.STATE_PLAYING,
                duration = metadata.duration,
                position = activeController?.playbackState?.position ?: 0
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
        activeController?.unregisterCallback(controllerCallback!!)
        activeController = null
    }
}
