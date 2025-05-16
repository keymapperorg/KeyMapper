package io.github.sds100.keymapper.system.accessibility

data class AccessibilityNodeAction(val action: Int, val extras: Map<String, Any?> = emptyMap())
