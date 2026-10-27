package com.nana.macros

import android.content.Context
import com.nana.common.Result
import com.nana.common.VoiceMacro
import com.nana.logging.NanaLogger
import com.nana.phone.PhoneController
import com.nana.phone.SystemToggles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes voice macros (trigger phrase -> action sequence).
 */
class MacroEngine(
    private val context: Context,
    private val macroDao: com.nana.storage.MacroDao,
    private val phoneController: PhoneController,
    private val mediaController: com.nana.media.MediaController? = null
) {
    suspend fun executeMacro(triggerPhrase: String): Result<Unit> = withContext(Dispatchers.IO) {
        val macro = macroDao.getByTrigger(triggerPhrase)
            ?: return@withContext Result.Failure(Result.Error("MACRO_NOT_FOUND", "Macro not found: $triggerPhrase"))
        
        if (!macro.enabled) {
            return@withContext Result.Failure(Result.Error("MACRO_DISABLED", "Macro is disabled"))
        }

        NanaLogger.i("MacroEngine", "Executing macro: ${macro.name}")
        
        for (action in macro.actions) {
            val result = executeAction(action)
            if (result.isFailure) {
                NanaLogger.e("MacroEngine", "Macro action failed: ${action.type}", result.getOrNull()?.error?.cause)
                return@withContext Result.Failure(Result.Error("MACRO_ACTION_FAILED", "Action ${action.type} failed: ${result.getOrNull()?.error?.message}"))
            }
        }

        macroDao.markUsed(triggerPhrase, System.currentTimeMillis())
        Result.Success(Unit)
    }

    private fun executeAction(action: VoiceMacro.MacroAction): Result<Unit> = when (action.type) {
        VoiceMacro.MacroAction.ActionType.OPEN_APP -> {
            val pkg = action.params["package"] ?: return Result.Failure(Result.Error("NO_PACKAGE", "Package required"))
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let { context.startActivity(it) }
            Result.Success(Unit)
        }
        VoiceMacro.MacroAction.ActionType.TOGGLE_SETTING -> {
            val setting = action.params["setting"] ?: return Result.Failure(Result.Error("NO_SETTING", "Setting required"))
            val value = action.params["value"]?.toBoolean() ?: true
            when (setting) {
                "wifi" -> SystemToggles.setWifiEnabled(context, value)
                "bluetooth" -> SystemToggles.setBluetoothEnabled(context, value)
                "dnd" -> SystemToggles.setDndEnabled(context, value)
                "flashlight" -> SystemToggles.setFlashlight(context, value)
                else -> Result.Failure(Result.Error("UNKNOWN_SETTING", "Unknown setting: $setting"))
            }
        }
        VoiceMacro.MacroAction.ActionType.MEDIA_CONTROL -> {
            val actionParam = action.params["action"] ?: return Result.Failure(Result.Error("NO_ACTION", "Media action required"))
            mediaController?.let { mc ->
                when (actionParam) {
                    "play" -> mc.play()
                    "pause" -> mc.pause()
                    "next" -> mc.next()
                    "previous" -> mc.previous()
                    else -> Result.Failure(Result.Error("UNKNOWN_MEDIA_ACTION", "Unknown media action: $actionParam"))
                }
            } ?: Result.Failure(Result.Error("NO_MEDIA", "Media controller not available"))
        }
        VoiceMacro.MacroAction.ActionType.SEND_MESSAGE -> {
            val contact = action.params["contact"] ?: return Result.Failure(Result.Error("NO_CONTACT", "Contact required"))
            val message = action.params["message"] ?: return Result.Failure(Result.Error("NO_MESSAGE", "Message required"))
            val app = action.params["app"] ?: "com.whatsapp"
            // Delegate to agent for full flow
            Result.Failure(Result.Error("DELEGATE_TO_AGENT", "Message sending requires agent"))
        }
        VoiceMacro.MacroAction.ActionType.SET_ALARM -> {
            val time = action.params["time"] ?: return Result.Failure(Result.Error("NO_TIME", "Time required"))
            val parts = time.split(":")
            val hour = parts[0].toIntOrNull() ?: return Result.Failure(Result.Error("INVALID_TIME", "Invalid hour"))
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            phoneController.setAlarm(hour, minute)
        }
        VoiceMacro.MacroAction.ActionType.SET_TIMER -> {
            val duration = action.params["duration"] ?: return Result.Failure(Result.Error("NO_DURATION", "Duration required"))
            val seconds = parseDuration(duration)
            phoneController.setTimer(seconds)
        }
        VoiceMacro.MacroAction.ActionType.TTS_SPEAK -> {
            val text = action.params["text"] ?: return Result.Failure(Result.Error("NO_TEXT", "Text required"))
            // Delegate to TTS manager
            Result.Failure(Result.Error("DELEGATE_TO_TTS", "TTS speak requires TTS manager"))
        }
        VoiceMacro.MacroAction.ActionType.WAIT -> {
            val ms = action.params["ms"]?.toLongOrNull() ?: 1000
            Thread.sleep(ms)
            Result.Success(Unit)
        }
        VoiceMacro.MacroAction.ActionType.CONDITION -> {
            // Conditional execution - simplified
            Result.Success(Unit)
        }
    }

    private fun parseDuration(input: String): Long {
        // Parse "5m", "30s", "1h", etc.
        return when {
            input.endsWith("h") -> input.removeSuffix("h").toLongOrNull()?.times(3600) ?: 0
            input.endsWith("m") -> input.removeSuffix("m").toLongOrNull()?.times(60) ?: 0
            input.endsWith("s") -> input.removeSuffix("s").toLongOrNull() ?: 0
            else -> input.toLongOrNull() ?: 0
        }
    }

    suspend fun createMacro(macro: VoiceMacro): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            macroDao.insert(macro)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("CREATE_FAILED", "Failed to create macro", e))
        }
    }

    suspend fun deleteMacro(trigger: String): Result<Unit> = withContext(Dispatchers.IO) {
        macroDao.delete(trigger)
        Result.Success(Unit)
    }
}
