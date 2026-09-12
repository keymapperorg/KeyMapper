package io.github.sds100.keymapper.base.system.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * How many nodes to check when looking for the node that can perform an action on behalf
 * of another node. This is only a safety net because the search normally stops at the
 * scrolling container.
 */
private const val MAX_ACTION_TARGET_DEPTH = 10

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

/**
 * Find the node that can perform [action] on behalf of this node.
 *
 * The node with the text or content description is often not the node that handles the
 * interaction. In a list row the text usually sits in a child of the container that
 * handles the click. Search up the tree for that container rather than assuming where it
 * is, because the layouts change subtly between Android devices and ROMs.
 *
 * @return The node to perform the action on, or null if there isn't one.
 */
fun AccessibilityNodeInfo.findActionTarget(
    action: Int,
    maxDepth: Int = MAX_ACTION_TARGET_DEPTH,
): AccessibilityNodeInfo? {
    var node: AccessibilityNodeInfo? = this
    var depth = 0

    while (node != null && depth <= maxDepth) {
        if (node.supportsAction(action)) {
            return node
        }

        // Stop at the scrolling container because performing the action on the whole
        // list would not do what the caller wants.
        if (node.isScrollable) {
            return null
        }

        node = node.parent
        depth++
    }

    return null
}

private fun AccessibilityNodeInfo.supportsAction(action: Int): Boolean =
    actionList.any { it.id == action }

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
