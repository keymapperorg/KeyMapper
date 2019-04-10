package io.github.sds100.keymapper.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

/**
 * Created by sds100 on 28/12/2018.
 */

object ImeUtils {
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