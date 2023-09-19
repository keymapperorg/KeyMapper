package io.github.sds100.keymapper.system.accessibility

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 17/04/2021.
 */
interface IAccessibilityService {
    fun doGlobalAction(action: Int): Result<*>

    fun tapScreen(x: Int, y: Int, inputEventType: InputEventType): Result<*>
    fun swipeScreen(xStart: Int, yStart: Int, xEnd: Int, yEnd: Int, fingerCount: Int, duration: Int, inputEventType: InputEventType): Result<*>
    fun tapScreenElement(fullName: String, onlyIfVisible: Boolean, inputEventType: InputEventType): Result<*>

    fun fetchAvailableUIElements(): List<String>
    fun clickOnElementWithId(id: String): Result<*>

    val isFingerprintGestureDetectionAvailable: Boolean

    var serviceFlags: Int?
    var serviceFeedbackType: Int?
    var serviceEventTypes: Int?
    
    fun performActionOnNode(
        findNode: (node: AccessibilityNodeModel) -> Boolean,
        performAction: (node: AccessibilityNodeModel) -> AccessibilityNodeAction?
    ): Result<*>

    val rootNode: AccessibilityNodeModel?

    fun hideKeyboard()
    fun showKeyboard()
    val isKeyboardHidden: Flow<Boolean>

    fun switchIme(imeId: String)

    @RequiresApi(Build.VERSION_CODES.N)
    fun disableSelf()

    fun findFocussedNode(focus: Int): AccessibilityNodeModel?
}