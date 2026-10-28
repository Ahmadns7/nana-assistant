package com.nana.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Offline OCR using ML Kit Text Recognition.
 * Works on images, screenshots, and unreadable UI elements.
 */
class OCREngine(private val context: Context) {
    private var textRecognizer: TextRecognizer? = null
    private var initialized = false

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val options = TextRecognizerOptions.Builder().build()
            textRecognizer = TextRecognition.getClient(options)
            initialized = true
            NanaLogger.i("OCREngine", "ML Kit Text Recognition initialized")
            Result.Success(Unit)
        } catch (e: Exception) {
            NanaLogger.e("OCREngine", "Initialization failed", e)
            Result.Failure(Result.Error("INIT_FAILED", "OCR initialization failed", e, recoverable = false))
        }
    }

    /**
     * Recognize text from a Bitmap.
     */
    suspend fun recognizeFromBitmap(bitmap: Bitmap): Result<OCRResult> = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext Result.Failure(Result.Error("NOT_INITIALIZED", "OCR not initialized"))
        
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer!!.process(image).await()
            
            val blocks = result.textBlocks.map { block ->
                OCRBlock(
                    text = block.text,
                    boundingBox = block.boundingBox,
                    cornerPoints = block.cornerPoints?.map { it.x to it.y } ?: emptyList(),
                    recognizedLanguage = block.recognizedLanguage
                )
            }
            
            val fullText = result.text
            NanaLogger.i("OCREngine", "Recognized ${blocks.size} blocks, ${fullText.length} chars")
            Result.Success(OCRResult(fullText, blocks))
        } catch (e: Exception) {
            Result.Failure(Result.Error("RECOGNITION_FAILED", "OCR failed", e))
        }
    }

    /**
     * Recognize text from media.Image (e.g., from camera or screen capture).
     */
    @RequiresApi(api = 29)
    suspend fun recognizeFromImage(image: Image, rotationDegrees: Int): Result<OCRResult> = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext Result.Failure(Result.Error("NOT_INITIALIZED", "OCR not initialized"))
        
        try {
            val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
            val result = textRecognizer!!.process(inputImage).await()
            
            val blocks = result.textBlocks.map { block ->
                OCRBlock(
                    text = block.text,
                    boundingBox = block.boundingBox,
                    cornerPoints = block.cornerPoints?.map { it.x to it.y } ?: emptyList(),
                    recognizedLanguage = block.recognizedLanguage
                )
            }
            
            Result.Success(OCRResult(result.text, blocks))
        } catch (e: Exception) {
            Result.Failure(Result.Error("RECOGNITION_FAILED", "OCR from image failed", e))
        }
    }

    /**
     * Recognize text from file path.
     */
    suspend fun recognizeFromFile(filePath: String): Result<OCRResult> = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext Result.Failure(Result.Error("NOT_INITIALIZED", "OCR not initialized"))
        
        try {
            val image = InputImage.fromFilePath(context, android.net.Uri.parse(filePath))
            val result = textRecognizer!!.process(image).await()
            
            val blocks = result.textBlocks.map { block ->
                OCRBlock(
                    text = block.text,
                    boundingBox = block.boundingBox,
                    cornerPoints = block.cornerPoints?.map { it.x to it.y } ?: emptyList(),
                    recognizedLanguage = block.recognizedLanguage
                )
            }
            
            Result.Success(OCRResult(result.text, blocks))
        } catch (e: Exception) {
            Result.Failure(Result.Error("RECOGNITION_FAILED", "OCR from file failed", e))
        }
    }

    fun release() {
        textRecognizer?.close()
        textRecognizer = null
        initialized = false
    }

    data class OCRResult(
        val fullText: String,
        val blocks: List<OCRBlock>
    )

    data class OCRBlock(
        val text: String,
        val boundingBox: Rect?,
        val cornerPoints: List<Pair<Int, Int>>,
        val recognizedLanguage: String?
    )
}
