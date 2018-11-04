package io.github.sds100.keymapper.Utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.*

/**
 * Created by sds100 on 03/09/2018.
 */

/**
 * Provides functions commonly used with [Action]s
 */
object ActionUtils {

    @IntDef(value = [
        ERROR_CODE_NO_ACTION_DATA,
        ERROR_CODE_ACTION_IS_NULL,
        ERROR_CODE_APP_DISABLED,
        ERROR_CODE_APP_UNINSTALLED,
        ERROR_CODE_PERMISSION_DENIED,
        ERROR_CODE_SHORTCUT_NOT_FOUND]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ActionErrorCode

    const val ERROR_CODE_NO_ACTION_DATA = 345
    const val ERROR_CODE_ACTION_IS_NULL = 123
    const val ERROR_CODE_PERMISSION_DENIED = 763
    const val ERROR_CODE_APP_DISABLED = 323
    const val ERROR_CODE_APP_UNINSTALLED = 452
    const val ERROR_CODE_SHORTCUT_NOT_FOUND = 329

    /**
     * Get a description for an action. E.g if the user chose an app, then the description will
     * be 'Open <app>'
     */
    fun getDescription(ctx: Context, action: Action?): ActionDescription {

        val errorCode = getErrorCode(ctx, action)

        val errorMessage = if (errorCode == null) {
            null
        } else {
            getMessageForErrorCode(ctx, errorCode, action)
        }

        val title = getTitle(ctx, action)
        val icon = getIcon(ctx, action)

        return ActionDescription(
                icon, title, errorMessage, errorCode
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
                val intent = Intent.parseUri(action.data, 0)

                //get the title for the shortcut
                if (intent.extras != null &&
                        intent.extras!!.containsKey(AppShortcutUtils.EXTRA_SHORTCUT_TITLE)) {
                    return intent.extras!!.getString(AppShortcutUtils.EXTRA_SHORTCUT_TITLE)
                }

                return null
            }

            ActionType.SYSTEM_ACTION -> {
                //convert the string representation into an enum
                val systemActionId = action.data
                return ctx.getString(SystemActionUtils.getDescription(systemActionId))
            }

            ActionType.KEYCODE -> {
                val key = KeyEvent.keyCodeToString(action.data.toInt())
                return ctx.getString(R.string.description_keycode, key)
            }

            ActionType.KEY -> {
                val keyCode = action.data.toInt()
                val key = KeycodeUtils.keycodeToString(keyCode)

                return ctx.getString(R.string.description_key, key)
            }

            ActionType.TEXT_BLOCK -> {
                val text = action.data
                return ctx.getString(R.string.description_text_block, text)
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
                val resId = SystemActionUtils.getIconResource(systemActionId)

                if (resId == null) return null

                ContextCompat.getDrawable(ctx, resId)
            }

            //return null if no icon should be used
            else -> null
        }
    }

    /**
     * if the action requires a permission, which needs user approval, this function
     * returns the permission required. Null is returned if the action doesn't need any permission
     */
    fun getRequiredPermissionForAction(action: Action): String? {
        if (action.type == ActionType.SYSTEM_ACTION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                return when (action.data) {
                    SystemAction.TOGGLE_AUTO_ROTATE -> Manifest.permission.WRITE_SETTINGS
                    SystemAction.ENABLE_AUTO_ROTATE -> Manifest.permission.WRITE_SETTINGS
                    SystemAction.DISABLE_AUTO_ROTATE -> Manifest.permission.WRITE_SETTINGS
                    SystemAction.PORTRAIT_MODE -> Manifest.permission.WRITE_SETTINGS
                    SystemAction.LANDSCAPE_MODE -> Manifest.permission.WRITE_SETTINGS
                    else -> null
                }
        }

        return null
    }

    fun fixActionError(ctx: Context, @ActionErrorCode errorCode: Int, action: Action) {
        when (errorCode) {
            ERROR_CODE_PERMISSION_DENIED -> {
                val requiredPermission = ActionUtils.getRequiredPermissionForAction(action)
                PermissionUtils.requestPermission(ctx, requiredPermission!!)
            }

            ERROR_CODE_APP_DISABLED -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${action.data}")
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                ctx.startActivity(intent)
            }

            ERROR_CODE_APP_UNINSTALLED -> {
                val packageName = action.data
                PackageUtils.viewAppOnline(ctx, packageName)
            }

            ERROR_CODE_SHORTCUT_NOT_FOUND -> {
                val intent = Intent.parseUri(action.data, 0)
                val packageName = intent.getStringExtra(AppShortcutUtils.EXTRA_PACKAGE_NAME)

                if (packageName != null) {
                    PackageUtils.viewAppOnline(ctx, packageName)
                }
            }
        }
    }

    /**
     * @return if the action can't be performed, it returns the string id of the error message to
     * explain why to the user. returns null if their if the action can be performed and there
     * is nothing wrong with it.
     */
    @ActionErrorCode
    private fun getErrorCode(ctx: Context, action: Action?): Int? {
        action ?: return ERROR_CODE_ACTION_IS_NULL
        if (action.data.isEmpty()) return ERROR_CODE_NO_ACTION_DATA

        if (!isRequiredPermissionGranted(ctx, action)) {
            return ERROR_CODE_PERMISSION_DENIED
        }

        when (action.type) {

            ActionType.APP -> {
                try {
                    val appInfo = ctx.packageManager.getApplicationInfo(action.data, 0)

                    //if the app is disabled, show an error message because it won't open
                    if (!appInfo.enabled) {
                        return ERROR_CODE_APP_DISABLED
                    }

                    return null
                } catch (e: Exception) {
                    return ERROR_CODE_APP_UNINSTALLED
                }
            }

            ActionType.APP_SHORTCUT -> {
                val intent = Intent.parseUri(action.data, 0)
                val activityExists = intent.resolveActivityInfo(ctx.packageManager, 0) != null

                if (!activityExists) {
                    return ERROR_CODE_SHORTCUT_NOT_FOUND
                }

                return null
            }

            else -> return null
        }
    }

    private fun isRequiredPermissionGranted(ctx: Context, action: Action): Boolean {
        //if the action doesn't need any special permissions
        val requiredPermission = getRequiredPermissionForAction(action) ?: return true

        return PermissionUtils.isPermissionGranted(ctx, requiredPermission)
    }

    private fun getMessageForErrorCode(ctx: Context,
                                       @ActionErrorCode errorCode: Int,
                                       action: Action?): String? {
        return when (errorCode) {
            ERROR_CODE_ACTION_IS_NULL -> ctx.getString(R.string.error_must_choose_action)
            ERROR_CODE_NO_ACTION_DATA -> ctx.getString(R.string.error_must_choose_action)
            ERROR_CODE_APP_DISABLED -> ctx.getString(R.string.error_app_is_disabled)
            ERROR_CODE_APP_UNINSTALLED -> ctx.getString(R.string.error_app_isnt_installed)
            ERROR_CODE_SHORTCUT_NOT_FOUND -> ctx.getString(R.string.error_shortcut_not_found)
            ERROR_CODE_PERMISSION_DENIED -> {
                val requiredPermission = getRequiredPermissionForAction(action!!)!!

                val permissionWarningMessage =
                        PermissionUtils.getPermissionWarningStringRes(requiredPermission)

                return ctx.getString(permissionWarningMessage)
            }

            else -> null
        }
    }
}