package io.github.sds100.keymapper.common.utils

import android.view.accessibility.AccessibilityNodeInfo

// DO NOT change these values, they are used in the data schema.
enum class NodeInteractionType(val accessibilityActionId: Int) {
    CLICK(AccessibilityNodeInfo.ACTION_CLICK),
    LONG_CLICK(AccessibilityNodeInfo.ACTION_LONG_CLICK),
    FOCUS(AccessibilityNodeInfo.ACTION_FOCUS),
    SCROLL_FORWARD(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),
    SCROLL_BACKWARD(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD),
    EXPAND(AccessibilityNodeInfo.ACTION_EXPAND),
    COLLAPSE(AccessibilityNodeInfo.ACTION_COLLAPSE),
}
