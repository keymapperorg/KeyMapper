package io.github.sds100.keymapper.system.accessibility

/**
 * Created by sds100 on 24/04/2021.
 */
data class AccessibilityNodeAction(val action: Int, val extras: Map<String, Any?> = emptyMap())
