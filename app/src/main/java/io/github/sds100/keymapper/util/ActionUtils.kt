package io.github.sds100.keymapper.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.service.MyIMEService
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_ACTION_IS_NULL
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_APP_DISABLED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_APP_UNINSTALLED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_BACK_FLASH_NOT_FOUND
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_FEATURE_NOT_AVAILABLE
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_FRONT_FLASH_NOT_FOUND
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_GOOGLE_APP_NOT_INSTALLED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_IME_SERVICE_DISABLED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_IME_SERVICE_NOT_CHOSEN
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_NO_ACTION_DATA
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_PERMISSION_DENIED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_SDK_VERSION_TOO_LOW
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_SHORTCUT_NOT_FOUND

/**
 * Created by sds100 on 03/09/2018.
 */

/**
 * Provides functions commonly used with [Action]s
 */
object ActionUtils {

    /**
     * Get a description for an action. E.g if the user chose an app, then the description will
     * be 'Open <app>'
     */
    fun getDescription(ctx: Context, action: Action?): ActionDescription {
        val errorResult = getError(ctx, action)

        //If the errorResult is null, errorMessage will be null
        val errorMessage = errorResult?.let { ErrorCodeUtils.getErrorCodeDescription(ctx, it) }

        val title = getTitle(ctx, action)
        val icon = getIcon(ctx, action)

        return ActionDescription(
            icon, title, errorMessage, errorResult
        )
    }

    private fun getTitle(ctx: Context, action: Action?): String? {
        action ?: return null

        when (action.type) {
            ActionType.APP -> {
                try {
                    val applicationInfo = ctx.packageManager.getApplicationInfo(
                        action.data,
                        PackageManager.GET_META_DATA
                    )

                    val applicationLabel = ctx.packageManager.getApplicationLabel(applicationInfo)

                    return ctx.getString(R.string.description_open_app, applicationLabel.toString())
                } catch (e: PackageManager.NameNotFoundException) {
                    //the app isn't installed
                    return null
                }
            }

            ActionType.APP_SHORTCUT -> {
                return action.getExtraData(Action.EXTRA_SHORTCUT_TITLE).onSuccess { it }
            }

            ActionType.SYSTEM_ACTION -> {
                val systemActionId = action.data

                return SystemActionUtils.getSystemActionDef(systemActionId).handle(
                    onSuccess = { systemActionDef ->

                        if (systemActionDef.getOptions(ctx).isFailure) {
                            systemActionDef.getDescription(ctx)
                        } else {
                            var optionLabel = Option.getOptionLabel(ctx, systemActionId, action.getOptionId())

                            //get a saved label for the option if it can't find one
                            if (optionLabel == null) {
                                when (systemActionId) {
                                }
                            }

                            optionLabel ?: return@handle systemActionDef.getDescription(ctx)

                            systemActionDef.getDescriptionWithOption(ctx, optionLabel!!)
                        }
                    },
                    onFailure = { null })
            }

            ActionType.URL -> {
                return ctx.str(R.string.description_url, action.data)
            }

            ActionType.KEYCODE -> {
                val key = KeyEvent.keyCodeToString(action.data.toInt())
                return ctx.str(R.string.description_keycode, key)
            }

            ActionType.KEY -> {
                val keyCode = action.data.toInt()
                val key = KeycodeUtils.keycodeToString(keyCode)

                return ctx.str(R.string.description_key, key)
            }

            ActionType.TEXT_BLOCK -> {
                val text = action.data
                return ctx.str(R.string.description_text_block, text)
            }
        }
    }

    /**
     * Get the icon for any Action
     */
    private fun getIcon(ctx: Context, action: Action?): Drawable? {
        action ?: return null

        return when (action.type) {
            ActionType.APP -> {
                try {
                    return ctx.packageManager.getApplicationIcon(action.data)
                } catch (e: PackageManager.NameNotFoundException) {
                    //if the app isn't installed, it can't find the icon for it
                    return null
                }
            }

            ActionType.SYSTEM_ACTION -> {
                //convert the string representation of the enum entry into an enum object
                val systemActionId = action.data

                SystemActionUtils.getSystemActionDef(systemActionId).onSuccess {
                    return@onSuccess it.getIcon(ctx)
                }
            }

            //return null if no icon should be used
            else -> null
        }
    }

    /**
     * @return if the action can't be performed, it returns an error code.
     * returns null if their if the action can be performed.
     */
    fun getError(ctx: Context, action: Action?): ErrorResult? {
        //action is null
        action ?: return ErrorResult(ERROR_CODE_ACTION_IS_NULL)

        //the action has not data
        if (action.data.isEmpty()) return ErrorResult(ERROR_CODE_NO_ACTION_DATA)

        if (action.requiresIME) {
            if (!MyIMEService.isServiceEnabled(ctx)) {
                return ErrorResult(ERROR_CODE_IME_SERVICE_DISABLED)
            }

            if (!MyIMEService.isInputMethodChosen(ctx)) {
                return ErrorResult(ERROR_CODE_IME_SERVICE_NOT_CHOSEN)
            }
        }

        when (action.type) {
            ActionType.APP -> {
                try {
                    val appInfo = ctx.packageManager.getApplicationInfo(action.data, 0)

                    //if the app is disabled, show an error message because it won't open
                    if (!appInfo.enabled) {
                        return ErrorResult(ERROR_CODE_APP_DISABLED, appInfo.packageName)
                    }

                    return null
                } catch (e: Exception) {
                    return ErrorResult(ERROR_CODE_APP_UNINSTALLED, action.data)
                }
            }

            ActionType.APP_SHORTCUT -> {
                val intent = Intent.parseUri(action.data, 0)
                val activityExists = intent.resolveActivityInfo(ctx.packageManager, 0) != null

                if (!activityExists) {
                    return ErrorResult(ERROR_CODE_SHORTCUT_NOT_FOUND, action.data)
                }
            }

            ActionType.SYSTEM_ACTION -> {
                val systemActionDef = SystemActionUtils.getSystemActionDef(action.data).data ?: return null

                //If an activity to open doesn't exist, the app crashes.
                if (systemActionDef.id == SystemAction.OPEN_ASSISTANT) {
                    val activityExists =
                        Intent(Intent.ACTION_VOICE_COMMAND).resolveActivityInfo(ctx.packageManager, 0) != null

                    if (!activityExists) {
                        return ErrorResult(ERROR_CODE_GOOGLE_APP_NOT_INSTALLED)
                    }
                }

                if (Build.VERSION.SDK_INT < systemActionDef.minApi) {
                    return ErrorResult(ERROR_CODE_SDK_VERSION_TOO_LOW, systemActionDef.minApi.toString())
                }

                systemActionDef.permissions.forEach { permission ->
                    if (!ctx.isPermissionGranted(permission)) {
                        return ErrorResult(ERROR_CODE_PERMISSION_DENIED, permission)
                    }
                }

                for (feature in systemActionDef.features) {
                    if (!ctx.packageManager.hasSystemFeature(feature)) {
                        return ErrorResult(ERROR_CODE_FEATURE_NOT_AVAILABLE, feature)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (systemActionDef.id == SystemAction.TOGGLE_FLASHLIGHT
                        || systemActionDef.id == SystemAction.ENABLE_FLASHLIGHT
                        || systemActionDef.id == SystemAction.DISABLE_FLASHLIGHT) {

                        action.getExtraData(Action.EXTRA_LENS).onSuccess { lensOptionId ->
                            val sdkLensId = Option.OPTION_ID_SDK_ID_MAP[lensOptionId]
                                ?: error("Can't find sdk id for that option id")

                            if (!CameraUtils.hasFlashFacing(ctx, sdkLensId)) {

                                when (lensOptionId) {
                                    Option.LENS_FRONT -> return@getError ErrorResult(ERROR_CODE_FRONT_FLASH_NOT_FOUND)
                                    Option.LENS_BACK -> return@getError ErrorResult(ERROR_CODE_BACK_FLASH_NOT_FOUND)
                                }
                            }
                        }
                    }
                }

                }

                return null
            }

            else -> return null
        }

        return null
    }
}