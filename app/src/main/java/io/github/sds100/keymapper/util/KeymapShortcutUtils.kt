package io.github.sds100.keymapper.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.DeviceInfo
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.activity.LaunchKeymapShortcutActivity
import io.github.sds100.keymapper.util.result.valueOrNull

/**
 * Created by sds100 on 21/01/21.
 */
object KeymapShortcutUtils {
    suspend fun createShortcut(
        activity: FragmentActivity,
        uuid: String,
        actionList: List<Action>,
        deviceInfoList: List<DeviceInfo>
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(activity, uuid).apply {

        val ctx: Context = activity
        val icon = createShortcutIcon(ctx, actionList)
        val shortcutLabel = createShortcutLabel(activity, actionList, deviceInfoList)

        setIcon(icon)
        setShortLabel(shortcutLabel)

        Intent(ctx, LaunchKeymapShortcutActivity::class.java).apply {
            action = MyAccessibilityService.ACTION_TRIGGER_KEYMAP_BY_UID
            putExtra(MyAccessibilityService.EXTRA_KEYMAP_UID, uuid)

            setIntent(this)
        }
    }.build()

    private fun createShortcutIcon(ctx: Context, actionList: List<Action>): IconCompat {
        if (actionList.size == 1) {
            val action = actionList[0]

            action.getIcon(ctx).valueOrNull()?.let {
                val bitmap = it.toBitmap()

                return IconCompat.createWithBitmap(bitmap)
            }
        }

        return IconCompat.createWithResource(ctx, R.mipmap.ic_launcher_round)
    }

    private suspend fun createShortcutLabel(
        activity: FragmentActivity,
        actionList: List<Action>,
        deviceInfoList: List<DeviceInfo>
    ): String {
        val ctx: Context = activity

        if (actionList.size == 1) {
            val action = actionList[0]

            action
                .getTitle(ctx, deviceInfoList)
                .valueOrNull()
                ?.let {
                    return it
                }
        }

        return activity.editTextStringAlertDialog(
            ctx.str(R.string.hint_shortcut_name),
            allowEmpty = false
        )
    }
}