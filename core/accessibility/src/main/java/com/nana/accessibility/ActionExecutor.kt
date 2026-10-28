package com.nana.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.nana.common.ScreenState
import com.nana.common.Result
import com.nana.logging.NanaLogger

/**
 * Executes planned actions via accessibility service.
 */
class ActionExecutor(private val service: NanaAccessibilityService) {

    /**
     * Execute a single action step.
     */
    fun execute(action: PlannedAction): Result<ActionResult> {
        return try {
            when (action.type) {
                ActionType.CLICK -> executeClick(action)
                ActionType.SCROLL -> executeScroll(action)
                ActionType.SET_TEXT -> executeSetText(action)
                ActionType.GLOBAL -> executeGlobal(action)
                ActionType.WAIT -> executeWait(action)
                ActionType.OPEN_APP -> executeOpenApp(action)
            }
        } catch (e: Exception) {
            NanaLogger.e("ActionExecutor", "Execution failed: ${action.type}", e)
            Result.Failure(Result.Error("EXECUTION_FAILED", "Action failed: ${e.message}", e))
        }
    }

    private fun executeClick(action: PlannedAction): Result<ActionResult> {
        val target = action.target ?: return Result.Failure(Result.Error("NO_TARGET", "Click target required"))
        val nodes = service.findNodes(text = target, contentDesc = target, id = target)
        
        if (nodes.isEmpty()) {
            return Result.Failure(Result.Error("NOT_FOUND", "Element not found: $target"))
        }
        
        val node = nodes.first()
        val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        nodes.forEach { it.recycle() }
        
        return if (success) {
            Result.Success(ActionResult(success = true, description = "Clicked $target"))
        } else {
            Result.Failure(Result.Error("CLICK_FAILED", "Click action failed"))
        }
    }

    private fun executeScroll(action: PlannedAction): Result<ActionResult> {
        val direction = action.params["direction"] ?: "forward"
        val target = action.target
        
        val success = if (target.isNotBlank()) {
            val nodes = service.findNodes(text = target)
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                val actionId = if (direction == "backward") AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                val result = node.performAction(actionId)
                nodes.forEach { it.recycle() }
                result
            } else false
        } else {
            val containers = ScreenReader(service).findScrollableContainers()
            if (containers.isNotEmpty()) {
                val container = containers.first()
                val actionId = if (direction == "backward") AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                val result = container.node.performAction(actionId)
                containers.forEach { it.node.recycle() }
                result
            } else false
        }
        
        return if (success) {
            Result.Success(ActionResult(success = true, description = "Scrolled $direction"))
        } else {
            Result.Failure(Result.Error("SCROLL_FAILED", "No scrollable element found"))
        }
    }

    private fun executeSetText(action: PlannedAction): Result<ActionResult> {
        val text = action.params["text"] ?: return Result.Failure(Result.Error("NO_TEXT", "Text parameter required"))
        val target = action.target ?: return Result.Failure(Result.Error("NO_TARGET", "Input target required"))
        
        val nodes = service.findNodes(text = target, contentDesc = target, id = target)
            .filter { it.isEditable || it.className.toString().contains("EditText") }
        
        if (nodes.isEmpty()) {
            return Result.Failure(Result.Error("NOT_FOUND", "Editable field not found: $target"))
        }
        
        val node = nodes.first()
        val success = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val bundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        } else false
        
        nodes.forEach { it.recycle() }
        
        return if (success) {
            Result.Success(ActionResult(success = true, description = "Set text: $text"))
        } else {
            Result.Failure(Result.Error("SET_TEXT_FAILED", "Failed to set text"))
        }
    }

    private fun executeGlobal(action: PlannedAction): Result<ActionResult> {
        val globalAction = action.params["action"] ?: return Result.Failure(Result.Error("NO_ACTION", "Global action required"))
        val success = when (globalAction) {
            "back" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            "recent" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            "notifications" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            "quick_settings" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            "power_dialog" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            else -> false
        }
        
        return if (success) {
            Result.Success(ActionResult(success = true, description = "Global action: $globalAction"))
        } else {
            Result.Failure(Result.Error("GLOBAL_FAILED", "Global action failed: $globalAction"))
        }
    }

    private fun executeWait(action: PlannedAction): Result<ActionResult> {
        val ms = action.params["ms"]?.toLongOrNull() ?: 1000
        Thread.sleep(ms)
        return Result.Success(ActionResult(success = true, description = "Waited ${ms}ms"))
    }

    private fun executeOpenApp(action: PlannedAction): Result<ActionResult> {
        val packageName = action.target ?: action.params["package"] ?: return Result.Failure(Result.Error("NO_PACKAGE", "Package name required"))
        
        val launchIntent = service.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            return Result.Failure(Result.Error("APP_NOT_FOUND", "App not installed: $packageName"))
        }
        
        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        service.startActivity(launchIntent)
        
        return Result.Success(ActionResult(success = true, description = "Opened app: $packageName"))
    }

    /**
     * Verify the result of an action by checking screen state.
     */
    fun verify(expectedChange: String, previousState: ScreenState): Result<Boolean> {
        val currentState = service.getCurrentScreenState() ?: return Result.Failure(Result.Error("NO_SCREEN", "Cannot get current screen"))
        val changed = currentState.hasChanged(previousState)
        
        // Simple heuristic: if screen changed, action likely had effect
        return Result.Success(changed)
    }

    sealed interface PlannedAction {
        val type: ActionType
        val params: Map<String, String>
        val target: String

        data class Click(override val target: String) : PlannedAction {
            override val type = ActionType.CLICK
            override val params = emptyMap<String, String>()
        }
        data class Scroll(override val target: String, val direction: String = "forward") : PlannedAction {
            override val type = ActionType.SCROLL
            override val params = mapOf("direction" to direction)
        }
        data class SetText(override val target: String, val text: String) : PlannedAction {
            override val type = ActionType.SET_TEXT
            override val params = mapOf("text" to text)
        }
        data class Global(val action: String) : PlannedAction {
            override val type = ActionType.GLOBAL
            override val params = mapOf("action" to action)
            override val target = ""
        }
        data class Wait(val ms: Long) : PlannedAction {
            override val type = ActionType.WAIT
            override val params = mapOf("ms" to ms.toString())
            override val target = ""
        }
        data class OpenApp(val packageName: String) : PlannedAction {
            override val type = ActionType.OPEN_APP
            override val params = mapOf("package" to packageName)
            override val target = packageName
        }
    }

    data class ActionResult(
        val success: Boolean,
        val description: String,
        val newScreenState: ScreenState? = null
    )

    enum class ActionType { CLICK, SCROLL, SET_TEXT, GLOBAL, WAIT, OPEN_APP }
}
