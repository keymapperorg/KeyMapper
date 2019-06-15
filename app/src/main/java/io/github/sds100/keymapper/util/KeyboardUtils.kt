package io.github.sds100.keymapper.util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.WidgetsManager

/**
 * Created by sds100 on 28/12/2018.
 */

object KeyboardUtils {
    fun showInputMethodPickerDialogOutsideApp(ctx: Context) {
        /* Android 8.1 and higher don't seem to allow you to open the input method picker dialog
             * from outside the app :( but it can be achieved by sending a broadcast with a
             * system process id (requires root access) */

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            imeManager.showInputMethodPicker()
        } else {
            val command = "am broadcast -a com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER"
            RootUtils.executeRootCommand(command)
        }
    }

    fun switchIme(imeId: String) {
        RootUtils.changeSecureSetting(Settings.Secure.DEFAULT_INPUT_METHOD, imeId)
    }
}

@RequiresApi(Build.VERSION_CODES.N)
fun AccessibilityService.SoftKeyboardController.hide(ctx: Context) {
    showMode = AccessibilityService.SHOW_MODE_HIDDEN
    WidgetsManager.onEvent(ctx, WidgetsManager.EVENT_HIDE_KEYBOARD)
}

@RequiresApi(Build.VERSION_CODES.N)
fun AccessibilityService.SoftKeyboardController.show(ctx: Context) {
    showMode = AccessibilityService.SHOW_MODE_AUTO
    WidgetsManager.onEvent(ctx, WidgetsManager.EVENT_SHOW_KEYBOARD)
}

@RequiresApi(Build.VERSION_CODES.N)
fun AccessibilityService.SoftKeyboardController.toggle(ctx: Context) {
    if (showMode == AccessibilityService.SHOW_MODE_HIDDEN) {
        show(ctx)
    } else {
        hide(ctx)
    }
}