package io.github.sds100.keymapper.base.system.accessibility

import io.github.sds100.keymapper.base.system.inputmethod.SwitchImeInterface
import io.github.sds100.keymapper.common.utils.InputEventAction
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.PinchScreenType
import kotlinx.coroutines.flow.Flow

interface IAccessibilityService : SwitchImeInterface {
    fun doGlobalAction(action: Int): KMResult<*>

    fun tapScreen(
        x: Int,
        y: Int,
        inputEventAction: InputEventAction,
    ): KMResult<*>

    fun swipeScreen(
        xStart: Int,
        yStart: Int,
        xEnd: Int,
        yEnd: Int,
        fingerCount: Int,
        duration: Int,
        inputEventAction: InputEventAction,
    ): KMResult<*>

    fun pinchScreen(
        x: Int,
        y: Int,
        distance: Int,
        pinchType: PinchScreenType,
        fingerCount: Int,
        duration: Int,
        inputEventAction: InputEventAction,
    ): KMResult<*>

    val isFingerprintGestureDetectionAvailable: Boolean

    var serviceFlags: Int?
    var serviceFeedbackType: Int?
    var serviceEventTypes: Int?
    var notificationTimeout: Long?

    fun performActionOnNode(
        findNode: (node: AccessibilityNodeModel) -> Boolean,
        performAction: (node: AccessibilityNodeModel) -> AccessibilityNodeAction?,
    ): KMResult<*>

    val rootNode: AccessibilityNodeModel?
    val activeWindowPackage: Flow<String?>
    val activeWindowPackageNames: List<String>

    fun hideKeyboard()

    fun showKeyboard()

    val isKeyboardHidden: Flow<Boolean>

    fun disableSelf()

    fun findFocussedNode(focus: Int): AccessibilityNodeModel?
}
