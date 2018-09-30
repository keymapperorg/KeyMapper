package io.github.sds100.keymapper.Utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.SystemAction

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
                val intent = Intent.parseUri(action.data, 0)

                //get the title for the shortcut
                if (intent.extras != null &&
                        intent.extras!!.containsKey(AppShortcutUtils.EXTRA_SHORTCUT_TITLE)) {
                    return intent.extras!!.getString(AppShortcutUtils.EXTRA_SHORTCUT_TITLE)
                }

                null
            }

            ActionType.SYSTEM_ACTION -> {
                //convert the string representation into an enum
                val systemActionEnum = SystemAction.valueOf(action.data)
                ctx.getString(SystemActionUtils.getDescription(systemActionEnum))
            }

            ActionType.KEYCODE -> {
                val key = KeyEvent.keyCodeToString(action.data.toInt())
                ctx.getString(R.string.description_keycode, key)
            }

            ActionType.KEY -> {
                val keyCode = action.data.toInt()
                val key = KeycodeUtils.keycodeToString(keyCode)
                ctx.getString(R.string.description_key, key)
            }

            ActionType.TEXT_BLOCK -> {
                val text = action.data
                ctx.getString(R.string.description_text_block, text)
            }
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
                val resId = SystemActionUtils.getIconResource(systemActionEnum)
                ContextCompat.getDrawable(ctx, resId)
            }

            //return null if no icon should be used
            else -> null
        }
    }
}