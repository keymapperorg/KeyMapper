package io.github.sds100.keymapper.base.system.navigation

import android.view.KeyEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityNodeAction
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.common.utils.InputEventType
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.system.inputevents.InputEventInjector
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OpenMenuHelper(
    private val suAdapter: SuAdapter,
    private val accessibilityService: IAccessibilityService,
    private val shizukuInputEventInjector: InputEventInjector,
    private val permissionAdapter: PermissionAdapter,
    private val coroutineScope: CoroutineScope,
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

                coroutineScope.launch {
                    shizukuInputEventInjector.inputKeyEvent(inputKeyModel)
                }

                return success()
            }

            suAdapter.isGranted.firstBlocking() ->
                return suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_MENU}\n")

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
