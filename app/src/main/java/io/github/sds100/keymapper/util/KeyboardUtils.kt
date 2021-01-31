package io.github.sds100.keymapper.util

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.O_MR1
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.result.*
import splitties.systemservices.inputMethodManager
import splitties.toast.toast

/**
 * Created by sds100 on 28/12/2018.
 */

object KeyboardUtils {
    //DON'T CHANGE THESE!!!
    private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP = "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_DOWN_UP"
    private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN = "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_DOWN"
    private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP = "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_UP"
    private const val KEY_MAPPER_INPUT_METHOD_ACTION_TEXT = "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_TEXT"

    private const val KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT = "io.github.sds100.keymapper.inputmethod.EXTRA_KEY_EVENT"
    private const val KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT = "io.github.sds100.keymapper.inputmethod.EXTRA_TEXT"

    const val KEY_MAPPER_GUI_IME_PACKAGE = "io.github.sds100.keymapper.inputmethod.latin"
    const val KEY_MAPPER_GUI_IME_MIN_API = Build.VERSION_CODES.KITKAT

    private const val SETTINGS_SECURE_SUBTYPE_HISTORY_KEY = "input_methods_subtype_history"

    val KEY_MAPPER_IME_PACKAGE_LIST = arrayOf(
        Constants.PACKAGE_NAME,
        KEY_MAPPER_GUI_IME_PACKAGE
    )

    fun enableCompatibleInputMethods(ctx: Context) {

        if (isPermissionGranted(ctx, Constants.PERMISSION_ROOT)) {
            enableCompatibleInputMethodsRoot()
        } else {
            openImeSettings(ctx)
        }
    }

    fun enableCompatibleInputMethodsRoot() {
        KEY_MAPPER_IME_PACKAGE_LIST.forEach {
            getImeId(it).onSuccess { imeId ->
                RootUtils.executeRootCommand("ime enable $imeId")
            }
        }
    }

    fun chooseCompatibleInputMethod(ctx: Context) {

        if (PermissionUtils.haveWriteSecureSettingsPermission(ctx)) {
            getLastUsedCompatibleImeId(ctx).onSuccess {
                switchIme(ctx, it)
                return
            }

            getImeId(Constants.PACKAGE_NAME).valueOrNull()?.let {
                switchIme(ctx, it)
                return
            }
        }

        showInputMethodPicker()
    }

    fun chooseLastUsedIncompatibleInputMethod(ctx: Context) {
        getLastUsedIncompatibleImeId(ctx).onSuccess {
            switchIme(ctx, it)
        }
    }

    fun toggleCompatibleIme(ctx: Context) {
        if (!isCompatibleImeEnabled()) {
            ctx.toast(R.string.error_ime_service_disabled)
            return
        }

        val imeId = if (isCompatibleImeChosen(ctx)) {
            getLastUsedIncompatibleImeId(ctx).valueOrNull()
        } else {
            getLastUsedCompatibleImeId(ctx).valueOrNull()
        }

        imeId ?: return

        //only show the toast message if it is successful
        if (switchIme(ctx, imeId)) {
            getInputMethodLabel(ctx, imeId).onSuccess { imeLabel ->
                toast(ctx.str(R.string.toast_chose_keyboard, imeLabel))
            }
        }
    }

    /**
     * @return whether the ime was changed successfully
     */
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    fun switchIme(ctx: Context, imeId: String): Boolean {
        if (!PermissionUtils.haveWriteSecureSettingsPermission(ctx)) {
            ctx.toast(R.string.error_need_write_secure_settings_permission)
            return false
        }

        SettingsUtils.putSecureSetting(ctx, Settings.Secure.DEFAULT_INPUT_METHOD, imeId)
        return true
    }

    fun showInputMethodPicker() {
        inputMethodManager.showInputMethodPicker()
    }

    fun showInputMethodPickerDialogOutsideApp(ctx: Context) {
        /* Android 8.1 and higher don't seem to allow you to open the input method picker dialog
             * from outside the app :( but it can be achieved by sending a broadcast with a
             * system process id (requires root access) */

        if (Build.VERSION.SDK_INT < O_MR1) {
            inputMethodManager.showInputMethodPicker()
        } else if ((O_MR1..Build.VERSION_CODES.P).contains(Build.VERSION.SDK_INT)) {
            val command = "am broadcast -a com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER"
            RootUtils.executeRootCommand(command)
        } else {
            ctx.toast(R.string.error_this_is_unsupported)
        }
    }

    fun getInputMethodLabel(ctx: Context, id: String): Result<String> {
        val label = inputMethodManager.enabledInputMethodList.find { it.id == id }
            ?.loadLabel(ctx.packageManager)?.toString() ?: return InputMethodNotFound(id)

        return Success(label)
    }

    fun getInputMethodIds(): Result<List<String>> {
        if (inputMethodManager.enabledInputMethodList.isEmpty()) {
            return NoEnabledInputMethods()
        }

        return Success(inputMethodManager.enabledInputMethodList.map { it.id })
    }

    fun getChosenInputMethodPackageName(ctx: Context): Result<String> {
        val chosenImeId = getChosenImeId(ctx)

        return getImePackageName(chosenImeId)
    }

    fun getImePackageName(imeId: String): Result<String> {
        val packageName = inputMethodManager.inputMethodList.find { it.id == imeId }?.packageName

        return if (packageName == null) {
            ImeNotFound(imeId)
        } else {
            Success(packageName)
        }
    }

    fun isImeEnabled(imeId: String): Boolean = getInputMethodIds().handle(
        onSuccess = { it.contains(imeId) },
        onFailure = { false }
    )

    fun isCompatibleImeEnabled(): Boolean {
        val enabledMethods = inputMethodManager.enabledInputMethodList ?: return false

        return enabledMethods.any { KEY_MAPPER_IME_PACKAGE_LIST.contains(it.packageName) }
    }

    fun isCompatibleImeChosen(ctx: Context): Boolean {
        return getChosenInputMethodPackageName(ctx)
            .then { Success(KEY_MAPPER_IME_PACKAGE_LIST.contains(it)) }
            .valueOrNull() ?: false
    }

    fun openImeSettings(ctx: Context) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK

            ctx.startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.error_cant_find_ime_settings)
        }
    }

    /**
     * Must verify that a compatible ime is being used before calling this.
     */
    fun inputTextFromImeService(ctx: Context, imePackageName: String, text: String) {
        Intent(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT).apply {
            setPackage(imePackageName)

            putExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT, text)
            ctx.sendBroadcast(this)
        }
    }

    /**
     * Must verify that a compatible ime is being used before calling this.
     */
    fun inputKeyEventFromImeService(
        ctx: Context,
        imePackageName: String,
        keyCode: Int,
        metaState: Int = 0,
        keyEventAction: KeyEventAction = KeyEventAction.DOWN_UP,
        deviceId: Int,
        scanCode: Int = 0
    ) {
        val intentAction = when (keyEventAction) {
            KeyEventAction.DOWN -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN
            KeyEventAction.DOWN_UP -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP
            KeyEventAction.UP -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP
        }

        Intent(intentAction).apply {
            setPackage(imePackageName)

            val action = when (keyEventAction) {
                KeyEventAction.DOWN, KeyEventAction.DOWN_UP -> KeyEvent.ACTION_DOWN
                KeyEventAction.UP -> KeyEvent.ACTION_UP
            }

            val eventTime = SystemClock.uptimeMillis()

            val keyEvent = KeyEvent(eventTime, eventTime, action, keyCode, 0, metaState, deviceId, scanCode)

            putExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT, keyEvent)

            ctx.sendBroadcast(this)
        }
    }

    private fun getChosenImeId(ctx: Context): String {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    }

    fun getImeId(packageName: String): Result<String> {
        val inputMethod = inputMethodManager.inputMethodList.find { it.packageName == packageName }
            ?: return KeyMapperImeNotFound()

        return Success(inputMethod.id)
    }

    private fun getSubtypeHistoryString(ctx: Context): String {
        return Settings.Secure.getString(ctx.contentResolver, SETTINGS_SECURE_SUBTYPE_HISTORY_KEY)
    }

    /**
     * Example:
     * io.github.sds100.keymapper.inputmethod.latin/.LatinIME;-921088104
     * :com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME;1891618174
     */
    private fun getInputMethodHistoryIds(ctx: Context): List<String> {
        return getSubtypeHistoryString(ctx)
            .split(':')
            .map { it.split(';')[0] }
    }

    private fun getLastUsedCompatibleImeId(ctx: Context): Result<String> {
        for (id in getInputMethodHistoryIds(ctx)) {
            if (id.split('/')[0] in KEY_MAPPER_IME_PACKAGE_LIST) {
                return Success(id)
            }
        }

        return getImeId(Constants.PACKAGE_NAME)
    }

    private fun getLastUsedIncompatibleImeId(ctx: Context): Result<String> {
        for (id in getInputMethodHistoryIds(ctx)) {
            if (id.split('/')[0] != Constants.PACKAGE_NAME) {
                return Success(id)
            }
        }

        return NoIncompatibleKeyboardsInstalled()
    }
}

@RequiresApi(Build.VERSION_CODES.N)
fun AccessibilityService.SoftKeyboardController.hide(ctx: Context) {
    showMode = AccessibilityService.SHOW_MODE_HIDDEN
    ServiceLocator.notificationController(ctx).onEvent(OnHideKeyboard)
}

@RequiresApi(Build.VERSION_CODES.N)
fun AccessibilityService.SoftKeyboardController.show(ctx: Context) {
    showMode = AccessibilityService.SHOW_MODE_AUTO
    ServiceLocator.notificationController(ctx).onEvent(OnShowKeyboard)
}

@RequiresApi(Build.VERSION_CODES.N)
fun AccessibilityService.SoftKeyboardController.toggle(ctx: Context) {
    if (showMode == AccessibilityService.SHOW_MODE_HIDDEN) {
        show(ctx)
    } else {
        hide(ctx)
    }
}