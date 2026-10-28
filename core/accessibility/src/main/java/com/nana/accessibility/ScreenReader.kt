package com.nana.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.nana.common.ScreenState
import com.nana.logging.NanaLogger

/**
 * High-level screen reading utilities.
 */
class ScreenReader(private val service: NanaAccessibilityService) {

    fun getScreenState(): ScreenState? = service.getCurrentScreenState()

    fun getScreenText(): String = service.getScreenText()

    /**
     * Find clickable elements on screen.
     */
    fun findClickableElements(): List<ClickableElement> {
        val nodes = service.findNodes()
        val result = nodes.mapNotNull { node ->
            if (node.isClickable || node.isFocusable || node.isLongClickable) {
                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                val id = node.viewIdResourceName ?: ""
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                ClickableElement(text, desc, id, bounds, node)
            } else null
        }
        result.forEach { if (!nodes.contains(it.node)) it.node.recycle() }
        return result
    }

    /**
     * Find text input fields.
     */
    fun findInputFields(): List<InputField> {
        val nodes = service.findNodes()
        val result = nodes.mapNotNull { node ->
            if (node.isEditable || node.className.toString().contains("EditText")) {
                val hint = node.contentDescription?.toString() ?: ""
                val id = node.viewIdResourceName ?: ""
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                InputField(hint, id, bounds, node)
            } else null
        }
        result.forEach { if (!nodes.contains(it.node)) it.node.recycle() }
        return result
    }

    /**
     * Find list/recycler views for scrolling.
     */
    fun findScrollableContainers(): List<ScrollableContainer> {
        val nodes = service.findNodes()
        val result = nodes.mapNotNull { node ->
            if (node.isScrollable) {
                val id = node.viewIdResourceName ?: ""
                val className = node.className.toString()
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                ScrollableContainer(id, className, bounds, node)
            } else null
        }
        result.forEach { if (!nodes.contains(it.node)) it.node.recycle() }
        return result
    }

    /**
     * Search for text on screen.
     */
    fun findText(query: String): List<AccessibilityNodeInfo> = service.findNodes(text = query)

    /**
     * Get structured summary of screen for LLM.
     */
    fun getScreenSummary(maxLength: Int = 500): String {
        val state = getScreenState() ?: return "No screen data"
        val clickables = findClickableElements().take(20)
        val inputs = findInputFields().take(10)

        val summary = StringBuilder()
        summary.append("App: ${state.packageName}/${state.activityName}\n")
        summary.append("Text: ${state.textSummary.take(maxLength)}\n")
        if (clickables.isNotEmpty()) {
            summary.append("Clickable: ${clickables.joinToString(", ") { it.description }}\n")
        }
        if (inputs.isNotEmpty()) {
            summary.append("Inputs: ${inputs.joinToString(", ") { it.hint }}\n")
        }
        return summary.toString()
    }

    data class ClickableElement(
        val text: String,
        val contentDesc: String,
        val id: String,
        val bounds: android.graphics.Rect,
        val node: AccessibilityNodeInfo
    ) {
        val description: String = if (text.isNotBlank()) text else if (contentDesc.isNotBlank()) contentDesc else id
    }

    data class InputField(
        val hint: String,
        val id: String,
        val bounds: android.graphics.Rect,
        val node: AccessibilityNodeInfo
    )

    data class ScrollableContainer(
        val id: String,
        val className: String,
        val bounds: android.graphics.Rect,
        val node: AccessibilityNodeInfo
    )
}
