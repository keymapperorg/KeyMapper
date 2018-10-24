package io.github.sds100.keymapper.Utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionDescription
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.R

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

        val errorMessage = getActionErrorMessage(ctx, action)

        //if there is no error message
        if (errorMessage == null) {
            return ActionDescription(
                    title = getTitle(ctx, action!!),
                    iconDrawable = getIcon(ctx, action)
            )
        } else {
            return ActionDescription(
                    errorMessage = ctx.getString(errorMessage)
            )
        }
    }

    private fun getTitle(ctx: Context, action: Action): String? {
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
        return when (action?.type) {
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
     * @return if the action can't be performed, it returns the string id of the error message to
     * explain why to the user. returns null if their if the action can be performed and there
     * is nothing wrong with it.
     */
    @StringRes
    private fun getActionErrorMessage(ctx: Context, action: Action?): Int? {
        if (action?.data.isNullOrEmpty()) return R.string.error_must_choose_action

        when (action!!.type) {
            ActionType.APP -> {
                try {
                    val appInfo = ctx.packageManager.getApplicationInfo(action.data, 0)

                    //if the app is disabled, show an error message because it won't open
                    if (!appInfo.enabled) {
                        return R.string.error_app_is_disabled
                    }
                    return null
                } catch (e: Exception) {
                    return R.string.error_app_isnt_installed
                }
            }

            ActionType.APP_SHORTCUT -> {
                val intent = Intent.parseUri(action.data, 0)
                val activityExists = intent.resolveActivityInfo(ctx.packageManager, 0) != null

                if (!activityExists) {
                    return R.string.error_shortcut_not_found
                }

                return null
            }

            else -> return null
        }
    }
}