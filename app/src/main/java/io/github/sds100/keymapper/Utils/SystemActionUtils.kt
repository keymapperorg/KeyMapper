package io.github.sds100.keymapper.Utils

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.SystemAction
import io.github.sds100.keymapper.SystemActionListItem

/**
 * Created by sds100 on 01/08/2018.
 */

object SystemActionUtils {

    val SYSTEM_ACTION_LIST_ITEMS = listOf(
            SystemActionListItem(SystemAction.TOGGLE_WIFI),
            SystemActionListItem(SystemAction.TOGGLE_BLUETOOTH)
    )

    /**
     * Get a string resource id for a string which describes a specified [SystemAction].
     */
    fun getDescription(systemAction: SystemAction): Int {
        return when (systemAction) {
            SystemAction.TOGGLE_WIFI -> R.string.action_toggle_wifi
            SystemAction.TOGGLE_BLUETOOTH -> R.string.action_toggle_bluetooth
            else -> 0
        }
    }

    /**
     * Get a drawable resource id for a specified [SystemAction]
     */
    fun getIconResource(systemAction: SystemAction): Int {
        return when (systemAction) {
            SystemAction.TOGGLE_WIFI -> R.drawable.ic_network_wifi_black_24dp
            SystemAction.TOGGLE_BLUETOOTH -> R.drawable.ic_bluetooth_black_24dp
            else -> 0
        }
    }
}