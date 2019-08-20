package io.github.sds100.keymapper.util

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.edit
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.result
import io.github.sds100.keymapper.service.MyIMEService
import org.jetbrains.anko.alert
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.okButton
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 28/12/2018.
 */

object KeyboardUtils {
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    fun switchToKeyMapperIme(ctx: Context) = ctx.apply {
        val shownWarningDialog = defaultSharedPreferences.getBoolean(
            str(R.string.key_pref_shown_cant_use_virtual_keyboard_message),
            bool(R.bool.default_value_shown_cant_use_virtual_keyboard_message)
        )

        fun switch() {
            if (!haveWriteSecureSettingsPermission) {
                toast(R.string.error_need_write_secure_settings_permission).show()
                return
            }

            if (MyIMEService.getImeId(ctx).result().isSuccess) {
                switchIme(ctx, MyIMEService.getImeId(ctx)!!)
            }
        }

        if (!shownWarningDialog) {
            alert {
                messageResource = R.string.dialog_message_cant_use_virtual_keyboard
                okButton {
                    defaultSharedPreferences.edit {
                        putBoolean(str(R.string.key_pref_shown_cant_use_virtual_keyboard_message), true)
                    }

                    switch()
                }
            }.show()
        } else {
            switch()
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    fun switchIme(ctx: Context, imeId: String) {
        ctx.putSecureSetting(Settings.Secure.DEFAULT_INPUT_METHOD, imeId)
    }

    fun openImeSettings(ctx: Context) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            ctx.startActivity(intent)
        } catch (e: Exception) {
            ctx.toast(R.string.error_cant_find_ime_settings)
        }
    }

    fun showInputMethodPicker(ctx: Context) {
        val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imeManager.showInputMethodPicker()
    }

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