package io.github.sds100.keymapper.interfaces

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Created by sds100 on 25/11/2018.
 */
interface IPerformAccessibilityAction {
    fun performGlobalAction(action: Int): Boolean

    val keyboardController: AccessibilityService.SoftKeyboardController?
    val rootNode: AccessibilityNodeInfo?
}