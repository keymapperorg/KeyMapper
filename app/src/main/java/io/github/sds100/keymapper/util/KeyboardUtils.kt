package io.github.sds100.keymapper.util

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.O_MR1
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.KeyMapperImeService
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.result.*
import splitties.init.appCtx
import splitties.systemservices.inputMethodManager
import splitties.toast.toast

/**
 * Created by sds100 on 28/12/2018.
 */

object KeyboardUtils {

    fun enableKeyMapperIme() {
        if (isPermissionGranted(Constants.PERMISSION_ROOT)) {
            KeyMapperImeService.getImeId().onSuccess {
                RootUtils.executeRootCommand("ime enable $it")
            }.onFailure {
            }
        } else {
            openImeSettings()
        }
    }

    fun switchToKeyMapperIme(ctx: Context) {
        if (!isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            ctx.toast(R.string.error_need_write_secure_settings_permission)
            return
        }

        KeyMapperImeService.getImeId().onSuccess {
            switchIme(it)
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    fun switchIme(imeId: String) {
        appCtx.putSecureSetting(Settings.Secure.DEFAULT_INPUT_METHOD, imeId)
    }

    fun showInputMethodPicker() {
        inputMethodManager.showInputMethodPicker()
    }

    fun showInputMethodPickerDialogOutsideApp() {
        /* Android 8.1 and higher don't seem to allow you to open the input method picker dialog
             * from outside the app :( but it can be achieved by sending a broadcast with a
             * system process id (requires root access) */

        if (Build.VERSION.SDK_INT < O_MR1) {
            inputMethodManager.showInputMethodPicker()
        } else if ((O_MR1..Build.VERSION_CODES.P).contains(Build.VERSION.SDK_INT)) {
            val command = "am broadcast -a com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER"
            RootUtils.executeRootCommand(command)
        } else {
            appCtx.toast(R.string.error_this_is_unsupported)
        }
    }

    fun getInputMethodLabel(id: String): Result<String> {
        val label = inputMethodManager.enabledInputMethodList.find { it.id == id }
            ?.loadLabel(appCtx.packageManager)?.toString() ?: return InputMethodNotFound(id)

        return Success(label)
    }

    fun getInputMethodIds(): Result<List<String>> {
        if (inputMethodManager.enabledInputMethodList.isEmpty()) {
            return NoEnabledInputMethods()
        }

        return Success(inputMethodManager.enabledInputMethodList.map { it.id })
    }

    fun inputMethodExists(imeId: String): Boolean = getInputMethodIds().handle(
        onSuccess = { it.contains(imeId) },
        onFailure = { false }
    )

    fun openImeSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK

            appCtx.startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.error_cant_find_ime_settings)
        }
    }

    fun sendDownUpFromImeService(
        keyCode: Int,
        metaState: Int = 0,
        keyEventAction: Int = KeyMapperImeService.ACTION_DOWN_UP
    ) {
        KeyMapperImeService.provideBus().value =
            Event(KeyMapperImeService.EVENT_INPUT_DOWN_UP to intArrayOf(keyCode, metaState, keyEventAction))
    }

    fun getChosenImeId(ctx: Context): String {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    }

    fun toggleKeyboard(ctx: Context) {
        if (!KeyMapperImeService.isServiceEnabled()) {
            ctx.toast(R.string.error_ime_service_disabled)
            return
        }

        if (KeyMapperImeService.isInputMethodChosen()) {
            AppPreferences.defaultIme?.let {
                switchIme(it)

                getInputMethodLabel(it).onSuccess { imeLabel ->
                    toast(ctx.str(R.string.toast_chose_keyboard, imeLabel))
                }
            }

        } else {
            AppPreferences.defaultIme = getChosenImeId(ctx)
            switchToKeyMapperIme(ctx)
            toast(R.string.toast_chose_keymapper_keyboard)
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