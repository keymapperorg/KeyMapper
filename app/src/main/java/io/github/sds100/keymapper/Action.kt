package io.github.sds100.keymapper

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import androidx.core.content.ContextCompat

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [data] The information required to perform the action. E.g if the type is [TYPE_APP]
 * then the data will be the package name of the application
 *
 * Different Types of actions:
 * - Applications
 * - Application shortcuts
 * - Keycode
 * - Insert a block of text
 * - System actions/settings
 * - Root actions
 */
data class Action(
        val type: ActionType,
        val data: String
) {
    companion object {
        const val EXTRA_ACTION = "extra_action"

        /**
         * Get a description for an action. E.g if the user chose an app, then the description will
         * be 'Open <app>'
         */
        fun getDescription(ctx: Context, action: Action): String? {
            return when (action.type) {
                ActionType.APP -> {
                    val applicationInfo = ctx.packageManager.getApplicationInfo(
                            action.data,
                            PackageManager.GET_META_DATA
                    )

                    val applicationLabel = ctx.packageManager.getApplicationLabel(applicationInfo)

                    return ctx.getString(R.string.description_open_app, applicationLabel.toString())
                }

                ActionType.APP_SHORTCUT -> {
                    val appShortcutList = AppShortcutHelper.getAppShortcuts(ctx.packageManager)
                    val appShortcut = appShortcutList.find { it.activityInfo.name == action.data }

                    return if (appShortcut == null) {
                        null
                    } else {
                        appShortcut.loadLabel(ctx.packageManager).toString()
                    }
                }

                ActionType.SYSTEM_ACTION -> {
                    //convert the string representation of the enum entry into an enum object
                    val systemActionEnum = SystemAction.valueOf(action.data)
                    ctx.getString(SystemActionHelper.getDescription(systemActionEnum))
                }

                ActionType.KEYCODE -> {
                    val key = KeyEvent.keyCodeToString(action.data.toInt())
                    ctx.getString(R.string.description_keycode, key)
                }

                ActionType.KEY -> {
                    val keyCode = action.data.toInt()
                    val key = KeycodeHelper.keycodeToString(keyCode)
                    ctx.getString(R.string.description_key, key)
                }

                ActionType.TEXT_BLOCK -> {
                    val text = action.data
                    ctx.getString(R.string.description_text_block, text)
                }

                else -> null
            }
        }

        /**
         * Get the icon for any Action
         */
        fun getIcon(ctx: Context, action: Action): Drawable? {
            return when (action.type) {
                ActionType.APP -> ctx.packageManager.getApplicationIcon(action.data)

                ActionType.SYSTEM_ACTION -> {
                    //convert the string representation of the enum entry into an enum object
                    val systemActionEnum = SystemAction.valueOf(action.data)
                    val resId = SystemActionHelper.getIconResource(systemActionEnum)
                    ContextCompat.getDrawable(ctx, resId)
                }

                ActionType.APP_SHORTCUT -> {
                    val appShortcutList = AppShortcutHelper.getAppShortcuts(ctx.packageManager)
                    val appShortcut = appShortcutList.find { it.activityInfo.name == action.data }

                    return if (appShortcut == null) {
                        null
                    } else {
                        appShortcut.loadIcon(ctx.packageManager)
                    }
                }

            //return null if no icon should be used
                else -> null
            }
        }
    }
}