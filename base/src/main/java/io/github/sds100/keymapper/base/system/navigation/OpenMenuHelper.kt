package io.github.sds100.keymapper.base.system.navigation

import android.view.InputDevice
import android.view.KeyEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityNodeAction
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.common.utils.then

class OpenMenuHelper(
    private val accessibilityService: IAccessibilityService,
    private val inputEventHub: InputEventHub,
) {

    companion object {
        private const val OVERFLOW_MENU_CONTENT_DESCRIPTION = "More options"
    }

    fun openMenu(): KMResult<*> {
        when {
            inputEventHub.isSystemBridgeConnected() -> {
                val downEvent = InjectKeyEventModel(
                    keyCode = KeyEvent.KEYCODE_MENU,
                    action = KeyEvent.ACTION_DOWN,
                    metaState = 0,
                    scanCode = 0,
                    deviceId = -1,
                    repeatCount = 0,
                    source = InputDevice.SOURCE_UNKNOWN,
                )

                val upEvent = downEvent.copy(action = KeyEvent.ACTION_UP)

                return inputEventHub.injectKeyEvent(downEvent).then {
                    inputEventHub.injectKeyEvent(upEvent)
                }
            }

            else -> {
                accessibilityService.performActionOnNode({ it.contentDescription == OVERFLOW_MENU_CONTENT_DESCRIPTION }) {
                    AccessibilityNodeAction(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                    )
                }

                return success()
            }
        }
    }
}
