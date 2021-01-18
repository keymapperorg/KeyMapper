package io.github.sds100.keymapper.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.os.Handler
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Created by sds100 on 25/11/2018.
 */
interface IAccessibilityService {
    fun performGlobalAction(action: Int): Boolean
    fun dispatchGesture(gesture: GestureDescription,
                        callback: GestureResultCallback?,
                        handler: Handler?): Boolean

    val keyboardController: AccessibilityService.SoftKeyboardController?
    val rootNode: AccessibilityNodeInfo?
    val fingerprintGestureDetectionAvailable: Boolean
    fun requestFingerprintGestureDetection()
    fun denyFingerprintGestureDetection()
}