package com.nana.agent

import android.content.Context
import com.nana.accessibility.ActionExecutor
import com.nana.accessibility.NanaAccessibilityService
import com.nana.accessibility.ScreenReader
import com.nana.common.ActionLog
import com.nana.common.Constants
import com.nana.common.Result
import com.nana.common.ScreenState
import com.nana.common.SessionContext
import com.nana.common.VoiceIntent
import com.nana.llm.LLMRunner
import com.nana.llm.PromptBuilder
import com.nana.logging.NanaLogger
import com.nana.media.MediaController
import com.nana.phone.PhoneController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main agent loop: observe -> plan -> act -> verify -> repeat.
 * Coordinates all subsystems to fulfill voice commands.
 */
class AgentLoop(
    private val context: Context,
    private val accessibilityService: NanaAccessibilityService,
    private val llmRunner: LLMRunner,
    private val mediaController: MediaController,
    private val phoneController: PhoneController,
    private val actionExecutor: ActionExecutor,
    private val screenReader: ScreenReader,
    private val sessionContext: SessionContext
) {
    private var running = false
    private var currentSessionId = ""
    private var stepCount = 0
    private var lastScreenState: ScreenState? = null

    suspend fun processIntent(intent: VoiceIntent, sessionId: String): Result<AgentResult> = withContext(Dispatchers.Default) {
        currentSessionId = sessionId
        stepCount = 0
        sessionContext.stepCount = 0
        NanaLogger.logSessionStart(sessionId, intent.toString())

        val startTime = System.currentTimeMillis()
        
        try {
            // Fast path for simple commands
            val fastPathResult = tryFastPath(intent)
            if (fastPathResult != null) {
                val duration = System.currentTimeMillis() - startTime
                logAction(intent, "fast_path", fastPathResult, duration, 0)
                NanaLogger.logSessionEnd(sessionId, duration, 1, fastPathResult.success)
                return@withContext Result.Success(AgentResult(fastPathResult.success, fastPathResult.message))
            }

            // Full agent loop
            val result = runAgentLoop(intent)
            val duration = System.currentTimeMillis() - startTime
            NanaLogger.logSessionEnd(sessionId, duration, stepCount, result.success)
            Result.Success(result)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            NanaLogger.e("AgentLoop", "Agent loop failed", e)
            NanaLogger.logSessionEnd(sessionId, duration, stepCount, false)
            Result.Failure(Result.Error("AGENT_FAILED", "Agent loop failed: ${e.message}", e))
        }
    }

    private fun tryFastPath(intent: VoiceIntent): ActionResult? {
        return when (intent) {
            is VoiceIntent.MediaControl -> handleMediaFastPath(intent)
            is VoiceIntent.AppControl -> handleAppFastPath(intent)
            is VoiceIntent.PhoneBasic -> handlePhoneFastPath(intent)
            is VoiceIntent.SettingsControl -> handleSettingsFastPath(intent)
            else -> null
        }
    }

    private fun handleMediaFastPath(intent: VoiceIntent.MediaControl): ActionResult {
        val action = intent.action
        val result = when (action) {
            VoiceIntent.MediaControl.MediaAction.PLAY -> mediaController.play()
            VoiceIntent.MediaControl.MediaAction.PAUSE -> mediaController.pause()
            VoiceIntent.MediaControl.MediaAction.NEXT -> mediaController.next()
            VoiceIntent.MediaControl.MediaAction.PREVIOUS -> mediaController.previous()
            VoiceIntent.MediaControl.MediaAction.WHATS_PLAYING -> {
                val info = mediaController.getNowPlayingInfo()
                Result.Success(info?.toSpokenString() ?: "Nothing is currently playing")
            }
            VoiceIntent.MediaControl.MediaAction.VOLUME_UP -> mediaController.adjustVolume(android.media.AudioManager.ADJUST_RAISE)
            VoiceIntent.MediaControl.MediaAction.VOLUME_DOWN -> mediaController.adjustVolume(android.media.AudioManager.ADJUST_LOWER)
            else -> Result.Failure(Result.Error("UNSUPPORTED", "Media action not supported"))
        }
        return result.fold(
            onSuccess = { msg -> ActionResult(true, msg as String) },
            onFailure = { ActionResult(false, "Media control failed: ${it.message}") }
        )
    }

    private fun handleAppFastPath(intent: VoiceIntent.AppControl): ActionResult {
        if (intent.action == VoiceIntent.AppControl.AppAction.OPEN) {
            val packageName = resolvePackageName(intent.target)
            return actionExecutor.execute(ActionExecutor.PlannedAction.OpenApp(packageName)).fold(
                onSuccess = { ActionResult(it.success, it.description) },
                onFailure = { ActionResult(false, "Failed to open app: ${it.message}") }
            )
        }
        return ActionResult(false, "App action not supported in fast path")
    }

    private fun handlePhoneFastPath(intent: VoiceIntent.PhoneBasic): ActionResult {
        val action = intent.action
        return when (action) {
            VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_FLASHLIGHT -> phoneController.toggleFlashlight()
            VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_WIFI -> phoneController.toggleWifi()
            VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_BLUETOOTH -> phoneController.toggleBluetooth()
            VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_DND -> phoneController.toggleDnd()
            VoiceIntent.PhoneBasic.PhoneAction.GET_BATTERY -> phoneController.getBatteryInfo()
            VoiceIntent.PhoneBasic.PhoneAction.GET_STORAGE -> phoneController.getStorageInfo()
            else -> ActionResult(false, "Phone action requires full agent")
        }
    }

    private fun handleSettingsFastPath(intent: VoiceIntent.SettingsControl): ActionResult {
        // Settings fast path - delegate to settings module
        return ActionResult(false, "Settings requires full agent")
    }

    private fun resolvePackageName(appName: String): String {
        // Fuzzy match against installed packages
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        val lowerName = appName.lowercase()
        
        return apps.firstOrNull { 
            pm.getApplicationLabel(it).toString().lowercase().contains(lowerName) ||
            it.packageName.lowercase().contains(lowerName)
        }?.packageName ?: appName
    }

    private suspend fun runAgentLoop(intent: VoiceIntent): AgentResult {
        lastScreenState = screenReader.getScreenState()
        
        while (stepCount < Constants.AGENT_MAX_STEPS) {
            stepCount++
            sessionContext.stepCount = stepCount
            
            // Check for pending confirmation
            if (sessionContext.pendingConfirmation != null) {
                if (sessionContext.pendingConfirmation!!.isExpired()) {
                    sessionContext.pendingConfirmation = null
                    return AgentResult(false, "Confirmation timed out")
                }
                // Wait for confirmation - in real implementation this would be event-driven
            }

            // Get current screen
            val screenState = screenReader.getScreenState() ?: return AgentResult(false, "Cannot read screen")
            lastScreenState = screenState

            // Build prompt for LLM
            val tools = getAvailableTools()
            val prompt = PromptBuilder.buildAgentPrompt(
                intent.toString(),
                screenReader.getScreenSummary(),
                sessionContext.referencedEntities.values.joinToString(", ") { "${it.type.name}:${it.displayName}" },
                tools
            )

            // Get LLM response
            val llmResult = llmRunner.generateResponse(prompt, sessionContext, tools)
            if (llmResult.isFailure) {
                return AgentResult(false, "LLM failed: ${llmResult.getOrNull()?.error?.message}")
            }

            val response = llmResult.getOrThrow()
            
            // Execute based on response type
            when (response) {
                is LLMRunner.LLMResponse.Action -> {
                    val actionResult = executeAction(response.action)
                    logAction(intent, response.action, actionResult, 0, stepCount)
                    
                    if (!actionResult.success) {
                        // Try verification
                        val verified = actionExecutor.verify(response.action, lastScreenState!!)
                        if (verified.getOrDefault(false)) {
                            continue // Action actually succeeded
                        }
                        return AgentResult(false, "Action failed: ${actionResult.description}")
                    }
                    
                    // Verify screen changed
                    val newScreen = screenReader.getScreenState()
                    if (newScreen != null && newScreen.hasChanged(screenState)) {
                        lastScreenState = newScreen
                        continue
                    }
                }
                is LLMRunner.LLMResponse.Speak -> {
                    return AgentResult(true, response.text)
                }
                is LLMRunner.LLMResponse.Ask -> {
                    // Need user input
                    sessionContext.pendingConfirmation = SessionContext.PendingConfirmation(
                        action = "user_input",
                        params = mapOf("question" to response.question),
                        description = response.question
                    )
                    return AgentResult(false, response.question)
                }
            }
        }

        return AgentResult(false, "Maximum steps reached. I'm stuck.")
    }

    private fun executeAction(actionString: String): ActionExecutor.ActionResult {
        // Parse action string like "CLICK(Play button)" or "SET_TEXT(message input, Hello)"
        return try {
            if (actionString.startsWith("CLICK(")) {
                val target = actionString.substring(6, actionString.length - 1)
                actionExecutor.execute(ActionExecutor.PlannedAction.Click(target)).getOrThrow()
            } else if (actionString.startsWith("SCROLL(")) {
                val parts = actionString.substring(7, actionString.length - 1).split(",")
                val target = parts[0].trim()
                val direction = parts.getOrNull(1)?.trim() ?: "forward"
                actionExecutor.execute(ActionExecutor.PlannedAction.Scroll(target, direction)).getOrThrow()
            } else if (actionString.startsWith("SET_TEXT(")) {
                val parts = actionString.substring(9, actionString.length - 1).split(",", limit = 2)
                val target = parts[0].trim()
                val text = parts.getOrNull(1)?.trim() ?: ""
                actionExecutor.execute(ActionExecutor.PlannedAction.SetText(target, text)).getOrThrow()
            } else if (actionString.startsWith("GLOBAL(")) {
                val action = actionString.substring(7, actionString.length - 1)
                actionExecutor.execute(ActionExecutor.PlannedAction.Global(action)).getOrThrow()
            } else if (actionString.startsWith("WAIT(")) {
                val ms = actionString.substring(5, actionString.length - 1).toLong()
                actionExecutor.execute(ActionExecutor.PlannedAction.Wait(ms)).getOrThrow()
            } else if (actionString.startsWith("OPEN_APP(")) {
                val packageName = actionString.substring(9, actionString.length - 1)
                actionExecutor.execute(ActionExecutor.PlannedAction.OpenApp(packageName)).getOrThrow()
            } else {
                ActionExecutor.ActionResult(false, "Unknown action: $actionString")
            }
        } catch (e: Exception) {
            ActionExecutor.ActionResult(false, "Action execution error: ${e.message}")
        }
    }

    private fun getAvailableTools(): List<String> {
        return listOf(
            "CLICK(text|contentDesc|id)",
            "SCROLL(target?, direction?)",
            "SET_TEXT(target, text)",
            "GLOBAL(back|home|recent|notifications|quick_settings)",
            "WAIT(ms)",
            "OPEN_APP(packageName)"
        )
    }

    private fun logAction(
        intent: VoiceIntent,
        action: String,
        result: ActionExecutor.ActionResult,
        durationMs: Long,
        step: Int
    ) {
        val log = ActionLog.create(
            intent = intent.toString(),
            category = VoiceIntent.categorize(intent),
            screenStateHash = lastScreenState?.hash ?: "unknown",
            actionTaken = action,
            actionParams = "",
            result = if (result.success) ActionLog.ActionResult.SUCCESS else ActionLog.ActionResult.FAILED,
            durationMs = durationMs,
            stepNumber = step,
            sessionId = currentSessionId
        )
        NanaLogger.logAction(log)
    }

    data class AgentResult(val success: Boolean, val message: String)
}
