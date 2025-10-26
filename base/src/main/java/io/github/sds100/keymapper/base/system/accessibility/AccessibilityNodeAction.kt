package io.github.sds100.keymapper.base.system.accessibility

data class AccessibilityNodeAction(
    val action: Int,
    val extras: Map<String, Any?> = emptyMap(),
)
