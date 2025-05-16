package io.github.sds100.keymapper.system.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * @return The node to find. Returns null if the node doesn't match the predicate
 */
fun AccessibilityNodeInfo?.findNodeRecursively(
    nodeInfo: AccessibilityNodeInfo? = this,
    depth: Int = 0,
    predicate: (node: AccessibilityNodeInfo) -> Boolean,
): AccessibilityNodeInfo? {
    if (nodeInfo == null) return null

    if (predicate(nodeInfo)) return nodeInfo

    for (i in 0 until nodeInfo.childCount) {
        val node = findNodeRecursively(nodeInfo.getChild(i), depth + 1, predicate)

        if (node != null) {
            return node
        }
    }

    return null
}

fun AccessibilityNodeInfo.toModel(): AccessibilityNodeModel = AccessibilityNodeModel(
    packageName = packageName?.toString(),
    contentDescription = contentDescription?.toString(),
    isFocused = isFocused,
    textSelectionStart = textSelectionStart,
    textSelectionEnd = textSelectionEnd,
    text = text?.toString(),
    isEditable = isEditable,
    className = className?.toString(),
    uniqueId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        uniqueId
    } else {
        null
    },
    viewResourceId = viewIdResourceName,
    actions = actionList.map { it.id },
)
