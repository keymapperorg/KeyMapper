package io.github.sds100.keymapper.system.accessibility

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.actions.pinchscreen.PinchScreenType
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.base.util.InputEventType
import kotlinx.coroutines.flow.Flow

interface IAccessibilityService {
    fun doGlobalAction(action: Int): Result<*>

    fun tapScreen(x: Int, y: Int, inputEventType: InputEventType): Result<*>

    fun swipeScreen(
        xStart: Int,
        yStart: Int,
        xEnd: Int,
        yEnd: Int,
        fingerCount: Int,
        duration: Int,
        inputEventType: InputEventType,
    ): Result<*>

    fun pinchScreen(
        x: Int,
        y: Int,
        distance: Int,
        pinchType: PinchScreenType,
        fingerCount: Int,
        duration: Int,
        inputEventType: InputEventType,
    ): Result<*>

    val isFingerprintGestureDetectionAvailable: Boolean

    var serviceFlags: Int?
    var serviceFeedbackType: Int?
    var serviceEventTypes: Int?
    var notificationTimeout: Long?

    fun performActionOnNode(
        findNode: (node: AccessibilityNodeModel) -> Boolean,
        performAction: (node: AccessibilityNodeModel) -> AccessibilityNodeAction?,
    ): Result<*>

    val rootNode: AccessibilityNodeModel?
    val activeWindowPackage: Flow<String?>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setInputMethodEnabled(imeId: String, enabled: Boolean)
    fun hideKeyboard()
    fun showKeyboard()
    val isKeyboardHidden: Flow<Boolean>

    fun switchIme(imeId: String)

    @RequiresApi(Build.VERSION_CODES.N)
    fun disableSelf()

    fun findFocussedNode(focus: Int): AccessibilityNodeModel?
}
