package io.github.sds100.keymapper.system.accessibility

/**
 * Created by sds100 on 21/04/2021.
 */
data class AccessibilityNodeModel(
    val packageName: String?,
    val contentDescription: String?,
    val isFocused: Boolean,
    val text: String?,
    val textSelectionStart: Int,
    val textSelectionEnd: Int,
    val isEditable: Boolean,
)
