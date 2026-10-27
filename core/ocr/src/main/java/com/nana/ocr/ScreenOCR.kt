package com.nana.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.view.Surface
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Screen OCR - captures screen and runs OCR on it.
 * Requires MediaProjection permission (user consent dialog).
 */
class ScreenOCR(
    private val context: Context,
    private val ocrEngine: OCREngine
) {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val handler = Handler(android.os.Looper.getMainLooper())

    /**
     * Start screen capture with user permission.
     */
    fun startCapture(mediaProjection: MediaProjection): Result<Unit> {
        this.mediaProjection = mediaProjection
        
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        val metrics = android.util.DisplayMetrics()
        display.getRealMetrics(metrics)
        
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            reader.acquireLatestImage()?.let { image ->
                // Process image async
                kotlinx.coroutines.Dispatchers.IO.asCoroutineDispatcher().launch {
                    recognizeScreen(image)
                }
            }
        }, handler)
        
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "NaNa Screen OCR",
            width, height, density,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, handler
        )
        
        return Result.Success(Unit)
    }

    private suspend fun recognizeScreen(image: Image) = withContext(Dispatchers.IO) {
        val result = ocrEngine.recognizeFromImage(image, 0)
        image.close()
        result.onSuccess { ocrResult ->
            NanaLogger.i("ScreenOCR", "Screen OCR: ${ocrResult.blocks.size} blocks")
            // Callback to UI or agent
        }
    }

    fun stopCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    /**
     * One-shot screen capture and OCR.
     */
    suspend fun captureAndRecognize(): Result<OCREngine.OCRResult> = withContext(Dispatchers.IO) {
        // This would use MediaProjectionManager to get one-time permission
        // For now, return not implemented
        Result.Failure(Result.Error("NOT_IMPLEMENTED", "One-shot capture requires MediaProjection permission flow"))
    }
}
