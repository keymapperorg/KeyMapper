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
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.result.*
import splitties.init.appCtx
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

    private const val KEY_MAPPER_INPUT_METHOD_EXTRA_KEYCODE = "io.github.sds100.keymapper.inputmethod.EXTRA_KEYCODE"
    private const val KEY_MAPPER_INPUT_METHOD_EXTRA_METASTATE = "io.github.sds100.keymapper.inputmethod.EXTRA_METASTATE"
    private const val KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT = "io.github.sds100.keymapper.inputmethod.EXTRA_TEXT"

    const val KEY_MAPPER_GUI_IME_PACKAGE = "io.github.sds100.keymapper.inputmethod.latin"
    const val KEY_MAPPER_GUI_IME_MIN_API = Build.VERSION_CODES.KITKAT

    val KEY_MAPPER_IME_PACKAGE_LIST = arrayOf(
        Constants.PACKAGE_NAME,
        KEY_MAPPER_GUI_IME_PACKAGE
    )

    fun enableCompatibleInputMethods() {

        if (isPermissionGranted(Constants.PERMISSION_ROOT)) {
            enableCompatibleInputMethodsRoot()
        } else {
            openImeSettings()
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

        if (isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            AppPreferences.lastUsedCompatibleImePackage?.let {
                getImeId(it).valueOrNull()?.let { imeId ->
                    switchIme(ctx, imeId)
                    return
                }
            }

            getImeId(Constants.PACKAGE_NAME).valueOrNull()?.let {
                switchIme(ctx, it)
                return
            }
        }

        showInputMethodPicker()
    }

    /**
     * @return whether the ime was changed successfully
     */
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    fun switchIme(ctx: Context, imeId: String): Boolean {
        if (!isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            ctx.toast(R.string.error_need_write_secure_settings_permission)
            return false
        }

        ctx.putSecureSetting(Settings.Secure.DEFAULT_INPUT_METHOD, imeId)
        return true
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

    /**
     * Must verify that a compatible ime is being used before calling this.
     */
    fun inputTextFromImeService(imePackageName: String, text: String) {
        Intent(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT).apply {
            setPackage(imePackageName)

            putExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT, text)
            appCtx.sendBroadcast(this)
        }
    }

    /**
     * Must verify that a compatible ime is being used before calling this.
     */
    fun inputKeyEventFromImeService(
        imePackageName: String,
        keyCode: Int,
        metaState: Int = 0,
        keyEventAction: KeyEventAction = KeyEventAction.DOWN_UP
    ) {
        val intentAction = when (keyEventAction) {
            KeyEventAction.DOWN -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN
            KeyEventAction.DOWN_UP -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP
            KeyEventAction.UP -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP
        }

        Intent(intentAction).apply {
            setPackage(imePackageName)
            putExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_KEYCODE, keyCode)
            putExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_METASTATE, metaState)

            appCtx.sendBroadcast(this)
        }
    }

    fun getChosenImeId(ctx: Context): String {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    }

    fun toggleCompatibleIme(ctx: Context) {
        if (!isCompatibleInputMethodEnabled()) {
            ctx.toast(R.string.error_ime_service_disabled)
            return
        }

        val imeId: String?

        if (isCompatibleInputMethodChosen(ctx)) {
            getChosenInputMethodPackageName(ctx).onSuccess {
                AppPreferences.lastUsedCompatibleImePackage = it
            }

            imeId = AppPreferences.lastUsedIncompatibleImeId ?: getFirstIncompatibleImeId(ctx)

        } else {
            saveLastUsedIncompatibleIme(ctx)

            imeId = getLastUsedCompatibleImeId().valueOrNull()
        }

        imeId ?: return

        //only show the toast message if it is successful
        if (switchIme(ctx, imeId)) {
            getInputMethodLabel(imeId).onSuccess { imeLabel ->
                toast(ctx.str(R.string.toast_chose_keyboard, imeLabel))
            }
        }
    }

    fun getImeId(packageName: String): Result<String> {
        val inputMethod = inputMethodManager.inputMethodList.find { it.packageName == packageName }
            ?: return KeyMapperImeNotFound()

        return Success(inputMethod.id)
    }

    fun isCompatibleInputMethodEnabled(): Boolean {
        val enabledMethods = inputMethodManager.enabledInputMethodList ?: return false

        return enabledMethods.any { KEY_MAPPER_IME_PACKAGE_LIST.contains(it.packageName) }
    }

    fun isCompatibleInputMethodChosen(ctx: Context): Boolean {
        return getChosenInputMethodPackageName(ctx)
            .then { Success(KEY_MAPPER_IME_PACKAGE_LIST.contains(it)) }
            .valueOrNull() ?: false
    }

    fun getChosenInputMethodPackageName(ctx: Context): Result<String> {
        val chosenImeId = getChosenImeId(ctx)

        return getImePackageName(chosenImeId)
    }

    fun getLastUsedCompatibleImeId(): Result<String> {
        val packageName = AppPreferences.lastUsedCompatibleImePackage ?: Constants.PACKAGE_NAME

        return getImeId(packageName)
    }

    fun getImePackageName(imeId: String): Result<String> {
        val packageName = inputMethodManager.inputMethodList.find { it.id == imeId }?.packageName

        return if (packageName == null) {
            ImeNotFound(imeId)
        } else {
            Success(packageName)
        }
    }

    fun saveLastUsedIncompatibleIme(ctx: Context) {
        var chosenImeId = getChosenImeId(ctx)

        getImePackageName(chosenImeId).onSuccess { chosenPackageName ->

            if (KEY_MAPPER_IME_PACKAGE_LIST.contains(chosenPackageName)) {
                getFirstIncompatibleImeId(ctx)?.let {
                    chosenImeId = it
                }
            }

            AppPreferences.lastUsedIncompatibleImeId = chosenImeId
        }
    }

    fun getFirstIncompatibleImeId(ctx: Context): String? {
        var incompatibleImeId: String? = null

        getInputMethodIds().onSuccess { imeList ->
            for (imeId in imeList) {
                var breakLoop = false

                getImePackageName(imeId).onSuccess {
                    if (!KEY_MAPPER_IME_PACKAGE_LIST.contains(it)) {
                        incompatibleImeId = imeId
                        breakLoop = true
                    }
                }

                if (breakLoop) {
                    break
                }
            }
        }

        return incompatibleImeId
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