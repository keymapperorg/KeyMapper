package io.github.sds100.keymapper.Utils

import android.os.Build
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.SystemAction
import io.github.sds100.keymapper.SystemActionListItem
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by sds100 on 01/08/2018.
 */

object SystemActionUtils {

    fun getSystemActionListItems(): List<SystemActionListItem> {
        return buildSequence {
            yield(SystemActionListItem(SystemAction.ENABLE_WIFI))
            yield(SystemActionListItem(SystemAction.DISABLE_WIFI))
            yield(SystemActionListItem(SystemAction.TOGGLE_WIFI))

            yield(SystemActionListItem(SystemAction.ENABLE_BLUETOOTH))
            yield(SystemActionListItem(SystemAction.DISABLE_BLUETOOTH))
            yield(SystemActionListItem(SystemAction.TOGGLE_BLUETOOTH))

            yield(SystemActionListItem(SystemAction.VOLUME_UP))
            yield(SystemActionListItem(SystemAction.VOLUME_DOWN))
            yield(SystemActionListItem(SystemAction.VOLUME_SHOW_DIALOG))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                yield(SystemActionListItem(SystemAction.VOLUME_MUTE))
                yield(SystemActionListItem(SystemAction.VOLUME_UNMUTE))
                yield(SystemActionListItem(SystemAction.VOLUME_TOGGLE_MUTE))
            }
        }.toList()
    }

    /**
     * Get a string resource id for a string which describes a specified [SystemAction].
     */
    fun getDescription(@SystemAction.SystemActionId systemAction: String): Int {
        return when (systemAction) {
            SystemAction.ENABLE_WIFI -> R.string.action_enable_wifi
            SystemAction.DISABLE_WIFI -> R.string.action_disable_wifi
            SystemAction.TOGGLE_WIFI -> R.string.action_toggle_wifi

            SystemAction.ENABLE_BLUETOOTH -> R.string.action_enable_bluetooth
            SystemAction.DISABLE_BLUETOOTH -> R.string.action_disable_bluetooth
            SystemAction.TOGGLE_BLUETOOTH -> R.string.action_toggle_bluetooth

            SystemAction.VOLUME_UP -> R.string.action_volume_up
            SystemAction.VOLUME_DOWN -> R.string.action_volume_down
            SystemAction.VOLUME_SHOW_DIALOG -> R.string.action_volume_show_dialog

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    when (systemAction) {
                        SystemAction.VOLUME_MUTE -> return R.string.action_volume_mute
                        SystemAction.VOLUME_TOGGLE_MUTE -> return R.string.action_toggle_mute
                        SystemAction.VOLUME_UNMUTE -> return R.string.action_volume_unmute
                    }
                }

                throw Exception("Can't find string for system action: $systemAction!")
            }
        }
    }

    /**
     * Get a drawable resource id for a specified [SystemAction]
     */
    fun getIconResource(@SystemAction.SystemActionId systemAction: String): Int? {
        return when (systemAction) {
            SystemAction.TOGGLE_WIFI -> R.drawable.ic_network_wifi_black_24dp
            SystemAction.ENABLE_WIFI -> R.drawable.ic_network_wifi_black_24dp
            SystemAction.DISABLE_WIFI -> R.drawable.ic_signal_wifi_off_black_24dp

            SystemAction.TOGGLE_BLUETOOTH -> R.drawable.ic_bluetooth_black_24dp
            SystemAction.ENABLE_BLUETOOTH -> R.drawable.ic_bluetooth_black_24dp
            SystemAction.DISABLE_BLUETOOTH -> R.drawable.ic_bluetooth_disabled_black_24dp

            SystemAction.VOLUME_UP -> R.drawable.ic_volume_up_black_24dp
            SystemAction.VOLUME_DOWN -> R.drawable.ic_volume_down_black_24dp
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    when (systemAction) {
                        SystemAction.VOLUME_MUTE -> return R.drawable.ic_volume_mute_black_24dp
                        SystemAction.VOLUME_TOGGLE_MUTE -> return R.drawable.ic_volume_mute_black_24dp
                        SystemAction.VOLUME_UNMUTE -> return R.drawable.ic_volume_up_black_24dp
                    }
                }

                return null
            }
        }
    }
}