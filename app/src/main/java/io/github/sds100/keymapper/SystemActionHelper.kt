package io.github.sds100.keymapper

/**
 * Created by sds100 on 01/08/2018.
 */

object SystemActionHelper {

    val SYSTEM_ACTION_LIST_ITEMS = listOf(
            SystemActionListItem(SystemAction.ACTION_TOGGLE_WIFI),
            SystemActionListItem(SystemAction.ACTION_TOGGLE_BLUETOOTH)
    )

    /**
     * Get a string resource id, which describes a particular [SystemActionHelper].
     */
    fun getDescription(systemAction: SystemAction): Int {
        return when (systemAction) {
            SystemAction.ACTION_TOGGLE_WIFI -> R.string.action_toggle_wifi
            SystemAction.ACTION_TOGGLE_BLUETOOTH -> R.string.action_toggle_bluetooth
            else -> 0
        }
    }

    /**
     *
     */
    fun getIconResource(systemAction: SystemAction): Int {
        return when (systemAction) {
            SystemAction.ACTION_TOGGLE_WIFI -> R.drawable.ic_network_wifi_black_24dp
            SystemAction.ACTION_TOGGLE_BLUETOOTH -> R.drawable.ic_bluetooth_black_24dp
            else -> 0
        }
    }
}