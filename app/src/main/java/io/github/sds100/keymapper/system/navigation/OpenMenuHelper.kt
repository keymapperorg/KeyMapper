package io.github.sds100.keymapper.system.navigation

import android.view.KeyEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import io.github.sds100.keymapper.system.accessibility.AccessibilityNodeAction
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 21/04/2021.
 */
class OpenMenuHelper(
    private val suAdapter: SuAdapter,
    private val accessibilityService: IAccessibilityService
) {

    companion object {
        private const val OVERFLOW_MENU_CONTENT_DESCRIPTION = "More options"
    }

    fun openMenu(): Result<*> {
        return if (suAdapter.isGranted.firstBlocking()) {
            suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_MENU}\n")
        } else {
            accessibilityService.performActionOnNode({ it.contentDescription == OVERFLOW_MENU_CONTENT_DESCRIPTION }) {
                AccessibilityNodeAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            }
        }
    }
}