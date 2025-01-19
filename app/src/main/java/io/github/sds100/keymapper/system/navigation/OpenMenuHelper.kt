package io.github.sds100.keymapper.system.navigation

import android.view.KeyEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import io.github.sds100.keymapper.system.accessibility.AccessibilityNodeAction
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.inputevents.InputEventInjector
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.success

/**
 * Created by sds100 on 21/04/2021.
 */
class OpenMenuHelper(
    private val suAdapter: SuAdapter,
    private val accessibilityService: IAccessibilityService,
    private val shizukuInputEventInjector: InputEventInjector,
    private val permissionAdapter: PermissionAdapter,
) {

    companion object {
        private const val OVERFLOW_MENU_CONTENT_DESCRIPTION = "More options"
    }

    fun openMenu(): Result<*> {
        when {
            permissionAdapter.isGranted(Permission.SHIZUKU) -> {
                val inputKeyModel = InputKeyModel(
                    keyCode = KeyEvent.KEYCODE_MENU,
                    inputType = InputEventType.DOWN_UP,
                )

                shizukuInputEventInjector.inputKeyEvent(inputKeyModel)

                return success()
            }

            suAdapter.isGranted.firstBlocking() ->
                return suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_MENU}\n")

            else -> {
                accessibilityService.performActionOnNode({ it.contentDescription == OVERFLOW_MENU_CONTENT_DESCRIPTION }) {
                    AccessibilityNodeAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                }

                return success()
            }
        }
    }
}
