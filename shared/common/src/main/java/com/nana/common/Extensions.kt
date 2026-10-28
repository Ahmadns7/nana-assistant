package com.nana.common

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

// Context extensions
fun Context.isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
    val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
    return enabledServices.contains(packageName + "/" + serviceClass.name)
}

fun Context.isNotificationListenerEnabled(serviceClass: Class<*>): Boolean {
    val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
    return enabledListeners.contains(packageName + "/" + serviceClass.name)
}

fun Context.isOverlayPermissionGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(this)
    } else true
}

fun Context.getModelsDir(): File = File(filesDir, Constants.MODELS_DIR).apply { mkdirs() }

fun Context.getModelPath(modelName: String): File = File(getModelsDir(), modelName)

// Coroutine extensions
suspend fun <T> withIO(block: () -> T): T = withContext(Dispatchers.IO) { block() }
suspend fun <T> withDefault(block: () -> T): T = withContext(Dispatchers.Default) { block() }

// AccessibilityNodeInfo extensions
fun AccessibilityNodeInfo.findFirst(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    if (predicate(this)) return this
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        val found = child.findFirst(predicate)
        if (found != null) return found
        child.recycle()
    }
    return null
}

fun AccessibilityNodeInfo.findAll(predicate: (AccessibilityNodeInfo) -> Boolean, result: MutableList<AccessibilityNodeInfo> = mutableListOf()): List<AccessibilityNodeInfo> {
    if (predicate(this)) result.add(this)
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        child.findAll(predicate, result)
        child.recycle()
    }
    return result
}

fun AccessibilityNodeInfo.getTextContent(): String {
    val texts = mutableListOf<String>()
    if (text?.isNotBlank() == true) texts.add(text.toString())
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        texts.add(child.getTextContent())
        child.recycle()
    }
    return texts.joinToString(" ")
}

fun AccessibilityNodeInfo.click(): Boolean {
    return performAction(AccessibilityNodeInfo.ACTION_CLICK)
}

fun AccessibilityNodeInfo.scrollForward(): Boolean {
    return performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
}

fun AccessibilityNodeInfo.scrollBackward(): Boolean {
    return performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
}

fun AccessibilityNodeInfo.setText(text: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        return performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) })
    }
    return false
}

fun AccessibilityNodeInfo.focus(): Boolean {
    return performAction(AccessibilityNodeInfo.ACTION_FOCUS)
}

// File extensions
fun File.readTextOrNull(): String? = if (exists()) readText() else null
fun File.writeTextIfChanged(text: String): Boolean {
    val current = readTextOrNull()
    if (current != text) {
        writeText(text)
        return true
    }
    return false
}

// Duration extensions
inline fun Long.toDurationString(): String = when {
    this < 1000 -> "${this}ms"
    this < 60000 -> "${this / 1000}s"
    this < 3600000 -> "${this / 60000}m ${(this % 60000) / 1000}s"
    else -> "${this / 3600000}h ${(this % 3600000) / 60000}m"
}
