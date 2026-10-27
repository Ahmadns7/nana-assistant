package com.nana.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.nana.common.Constants
import com.nana.common.ScreenState
import com.nana.logging.NanaLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * System AccessibilityService for screen reading and control.
 * Requires user to enable in Settings > Accessibility > NaNa Assistant.
 */
class NanaAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var screenCallback: ((ScreenState) -> Unit)? = null
    private var rootNode: AccessibilityNodeInfo? = null
    private var lastEventTime = 0L

    companion object {
        const val ACTION_GET_SCREEN = "com.nana.accessibility.GET_SCREEN"
        const val ACTION_PERFORM_ACTION = "com.nana.accessibility.PERFORM_ACTION"
        const val ACTION_CLICK = "click"
        const val ACTION_SCROLL = "scroll"
        const val ACTION_SET_TEXT = "set_text"
        const val ACTION_GLOBAL = "global"
        const val EXTRA_ACTION = "action"
        const val EXTRA_PARAMS = "params"
        const val EXTRA_RESULT = "result"
    }

    override fun onCreate() {
        super.onCreate()
        NanaLogger.init(BuildConfig.DEBUG)
        NanaLogger.i("NanaAccessibilityService", "Service created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            // Throttle events to avoid spam
            val now = System.currentTimeMillis()
            if (now - lastEventTime < 500) return
            lastEventTime = now

            NanaLogger.logAccessibilityEvent(it.eventType, it.packageName?.toString() ?: "", it.className?.toString() ?: "")
            
            // Update root node on significant events
            if (it.eventType in Constants.A11Y_EVENT_TYPES) {
                rootNode?.recycle()
                rootNode = getRootInActiveWindow()
                if (rootNode != null) {
                    val screenState = ScreenState.fromRoot(rootNode!!)
                    screenState?.let { screenCallback?.invoke(it) }
                }
            }
        }
    }

    override fun onInterrupt() {
        NanaLogger.w("NanaAccessibilityService", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            }
            notificationTimeout = 100
        }
        setServiceInfo(info)
        NanaLogger.i("NanaAccessibilityService", "Service connected")
    }

    override fun onDestroy() {
        rootNode?.recycle()
        rootNode = null
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    fun setScreenCallback(callback: (ScreenState) -> Unit) {
        screenCallback = callback
    }

    fun getCurrentScreenState(): ScreenState? {
        rootNode?.recycle()
        rootNode = getRootInActiveWindow()
        return rootNode?.let { ScreenState.fromRoot(it) }
    }

    /**
     * Perform an accessibility action.
     */
    fun performAction(action: String, params: Map<String, String>): Boolean {
        return when (action) {
            ACTION_CLICK -> clickAt(params["text"] ?: "", params["contentDesc"] ?: "", params["id"] ?: "")
            ACTION_SCROLL -> scroll(params["direction"] ?: "forward", params["text"] ?: "")
            ACTION_SET_TEXT -> setText(params["text"] ?: "", params["value"] ?: "")
            ACTION_GLOBAL -> globalAction(params["action"] ?: "")
            else -> false
        }
    }

    private fun clickAt(text: String, contentDesc: String, id: String): Boolean {
        val node = findNode(text, contentDesc, id) ?: return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        node.recycle()
        return result
    }

    private fun scroll(direction: String, text: String): Boolean {
        val node = if (text.isNotBlank()) findNode(text, "", "") else getScrollableRoot()
        node?.let {
            val action = if (direction == "backward") AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            val result = it.performAction(action)
            it.recycle()
            return result
        }
        return false
    }

    private fun getScrollableRoot(): AccessibilityNodeInfo? {
        return rootNode?.findFirst { it.isScrollable }
    }

    private fun setText(text: String, value: String): Boolean {
        val node = findNode(text, "", "") ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            node.recycle()
            return result
        }
        node.recycle()
        return false
    }

    private fun globalAction(action: String): Boolean {
        return when (action) {
            "back" -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            "recent" -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            "notifications" -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            "quick_settings" -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            "power_dialog" -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            else -> false
        }
    }

    private fun findNode(text: String, contentDesc: String, id: String): AccessibilityNodeInfo? {
        val root = getRootInActiveWindow() ?: return null
        return root.findFirst { node ->
            val matchText = text.isBlank() || node.text?.toString().lowercase().contains(text.lowercase()) == true
            val matchDesc = contentDesc.isBlank() || node.contentDescription?.toString().lowercase().contains(contentDesc.lowercase()) == true
            val matchId = id.isBlank() || node.viewIdResourceName?.contains(id) == true
            matchText && matchDesc && matchId
        }
    }

    /**
     * Find all nodes matching criteria.
     */
    fun findNodes(text: String = "", contentDesc: String = "", id: String = ""): List<AccessibilityNodeInfo> {
        val root = getRootInActiveWindow() ?: return emptyList()
        return root.findAll { node ->
            val matchText = text.isBlank() || node.text?.toString().lowercase().contains(text.lowercase()) == true
            val matchDesc = contentDesc.isBlank() || node.contentDescription?.toString().lowercase().contains(contentDesc.lowercase()) == true
            val matchId = id.isBlank() || node.viewIdResourceName?.contains(id) == true
            matchText && matchDesc && matchId
        }
    }

    /**
     * Get text content of current screen.
     */
    fun getScreenText(): String {
        return rootNode?.getTextContent() ?: ""
    }
}
