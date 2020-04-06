package io.github.sds100.keymapper.util

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.O_MR1
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.KeyMapperImeService
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.result.*
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.coroutines.showAndAwait
import splitties.alertdialog.appcompat.messageResource
import splitties.experimental.ExperimentalSplittiesApi
import splitties.init.appCtx
import splitties.systemservices.inputMethodManager
import splitties.toast.toast

/**
 * Created by sds100 on 28/12/2018.
 */

object KeyboardUtils {
    @ExperimentalSplittiesApi
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    suspend fun switchToKeyMapperIme() {
        val shownWarningDialog = AppPreferences.shownKeyMapperImeWarningDialog
        var approvedWarning = shownWarningDialog

        if (!shownWarningDialog) {
            approvedWarning = appCtx.alertDialog {
                messageResource = R.string.dialog_message_cant_use_virtual_keyboard
            }.showAndAwait(okValue = true,
                cancelValue = false,
                dismissValue = false)

            AppPreferences.shownKeyMapperImeWarningDialog = approvedWarning
        }

        if (!approvedWarning) {
            return
        }

        if (!isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            toast(R.string.error_need_write_secure_settings_permission)
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
        }else{
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
            Log.e(this::class.java.simpleName, e.toString())
            toast(R.string.error_cant_find_ime_settings)
        }
    }
}