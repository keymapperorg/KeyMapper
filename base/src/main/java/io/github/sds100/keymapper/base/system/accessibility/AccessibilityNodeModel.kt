package io.github.sds100.keymapper.base.system.accessibility

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable

@Serializable
data class AccessibilityNodeModel(
    val packageName: String?,
    val contentDescription: String?,
    val isFocused: Boolean,
    val text: String?,
    val textSelectionStart: Int,
    val textSelectionEnd: Int,
    val isEditable: Boolean,
    val className: String?,
    val viewResourceId: String?,
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val uniqueId: String?,
    /**
     * A list of the allowed accessibility node actions.
     */
    val actions: List<Int>,
)
