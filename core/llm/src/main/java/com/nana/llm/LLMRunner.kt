package com.nana.llm

import android.content.Context
import com.nana.common.Constants
import com.nana.common.ModelInfo
import com.nana.common.Result
import com.nana.common.getModelsDir
import com.nana.common.SessionContext
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device LLM inference runner.
 * Supports MediaPipe LLM Inference and llama.cpp via JNI.
 */
class LLMRunner(
    private val context: Context,
    private val modelInfo: ModelInfo
) {
    private var initialized = false
    private var session: Any? = null // MediaPipe LlmInference or llama.cpp context
    private val systemPrompt = buildSystemPrompt()

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelPath = resolveModelPath()
            if (!java.io.File(modelPath).exists()) {
                return@withContext Result.Failure(Result.Error("MODEL_NOT_FOUND", "LLM model not found at $modelPath"))
            }

            // TODO: Initialize based on model type
            // For MediaPipe LLM Inference:
            // val options = LlmInference.LlmInferenceOptions.builder()
            //     .setModelPath(modelPath)
            //     .setMaxTokens(Constants.LLM_MAX_TOKENS)
            //     .setTemperature(Constants.LLM_TEMPERATURE)
            //     .setTopK(40)
            //     .setTopP(Constants.LLM_TOP_P)
            //     .build()
            // session = LlmInference.createFromOptions(context, options)

            // For llama.cpp:
            // session = LlamaContext.create(modelPath, params)

            initialized = true
            NanaLogger.i("LLMRunner", "LLM initialized: ${modelInfo.id} at $modelPath")
            Result.Success(Unit)
        } catch (e: Exception) {
            NanaLogger.e("LLMRunner", "Initialization failed", e)
            Result.Failure(Result.Error("INIT_FAILED", "LLM initialization failed", e, recoverable = false))
        }
    }

    private fun resolveModelPath(): String {
        val modelsDir = context.getModelsDir()
        return File(modelsDir, modelInfo.path).absolutePath
    }

    private fun buildSystemPrompt(): String {
        return """You are NaNa, an offline voice assistant for Android. You control the phone via accessibility services.
Rules:
- Keep responses concise (1-2 sentences max for voice)
- Never guess - use tools to get real phone state
- Ask for confirmation before irreversible actions (send, call, delete)
- If you can't do something, say so clearly
- Format: [ACTION] or [RESPONSE] or [QUESTION]

Available actions:
- OPEN_APP(packageName)
- MEDIA_CONTROL(action, target)
- SEND_MESSAGE(contact, text, app)
- CALL(number)
- SET_ALARM(time)
- SET_TIMER(duration)
- TOGGLE_SETTING(setting, value)
- SCREEN_ACTION(action, target)
- WAIT(ms)

Current screen context will be provided before each turn.
""".trimIndent()
    }

    /**
     * Generate response for a user intent with screen context.
     */
    suspend fun generateResponse(
        userIntent: String,
        screenContext: SessionContext?,
        availableTools: List<String>
    ): Result<LLMResponse> = withContext(Dispatchers.Default) {
        if (!initialized) return@withContext Result.Failure(Result.Error("NOT_INITIALIZED", "LLM not initialized"))

        val startTime = System.currentTimeMillis()
        try {
            val prompt = buildPrompt(userIntent, screenContext, availableTools)
            
            // TODO: Run inference
            // val response = if (session is LlmInference) session.generateResponse(prompt) 
            // else if (session is LlamaContext) session.generate(prompt)
            
            // Placeholder response for now
            val response = "[RESPONSE] I understand. Let me help with that."
            
            val latency = System.currentTimeMillis() - startTime
            NanaLogger.logModelInference(modelInfo.id, prompt.length, response.length, latency, true)
            
            Result.Success(parseResponse(response))
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            NanaLogger.logModelInference(modelInfo.id, 0, 0, latency, false)
            Result.Failure(Result.Error("INFERENCE_FAILED", "LLM inference failed", e))
        }
    }

    private fun buildPrompt(
        userIntent: String,
        screenContext: SessionContext?,
        availableTools: List<String>
    ): String {
        val screenInfo = screenContext?.lastScreenState?.let { 
            "Screen: ${it.packageName}/${it.activityName} - ${it.textSummary}" 
        } ?: "Screen: Unknown"

        val toolsInfo = "Available tools: ${availableTools.joinToString(", ")}"
        val entities = screenContext?.referencedEntities?.values?.joinToString(", ") { 
            "${it.type.name.toLowerCase()}:${it.displayName}" 
        } ?: "None"

        return """
$systemPrompt

$screenInfo
Referenced entities: $entities
$toolsInfo

User: $userIntent
Assistant:""".trimIndent()
    }

    private fun parseResponse(raw: String): LLMResponse {
        // Parse structured response
        return if (raw.startsWith("[ACTION]")) {
            val action = raw.substring(8).trim()
            LLMResponse.Action(action)
        } else if (raw.startsWith("[RESPONSE]")) {
            val text = raw.substring(10).trim()
            LLMResponse.Speak(text)
        } else if (raw.startsWith("[QUESTION]")) {
            val question = raw.substring(10).trim()
            LLMResponse.Ask(question)
        } else {
            LLMResponse.Speak(raw)
        }
    }

    fun release() {
        session = null
        initialized = false
    }

    sealed interface LLMResponse {
        data class Action(val action: String) : LLMResponse
        data class Speak(val text: String) : LLMResponse
        data class Ask(val question: String) : LLMResponse
    }
}
