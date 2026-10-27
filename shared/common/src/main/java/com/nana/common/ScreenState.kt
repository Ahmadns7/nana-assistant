package com.nana.common

import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.Serializable

@Serializable
data class ScreenState(
    val packageName: String,
    val activityName: String,
    val nodeCount: Int,
    val textSummary: String,
    val hash: String,
    val timestamp: Long = System.currentTimeMillis(),
    val scrollable: Boolean = false,
    val focusedNodeText: String? = nil
) {
    companion object {
        fun fromRoot(root: AccessibilityNodeInfo?): ScreenState? {
            root ?: return null
            val text = root.getTextContent().take(500)
            val hash = (root.packageName.toString() + root.className.toString() + text).hashCode().toString()
            val scrollable = root.findFirst { it.isScrollable } != null
            val focusedNode = root.findFirst { it.isFocused }
            val focusedText = focusedNode?.text?.toString()
            focusedNode?.recycle()
            return ScreenState(
                packageName = root.packageName.toString(),
                activityName = root.className.toString(),
                nodeCount = countNodes(root),
                textSummary = text,
                hash = hash,
                scrollable = scrollable,
                focusedNodeText = focusedText
            )
        }

        private fun countNodes(node: AccessibilityNodeInfo): Int {
            var count = 1
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                count += countNodes(child)
                child.recycle()
            }
            return count
        }
    }

    fun hasChanged(other: ScreenState): Boolean = hash != other.hash
}
