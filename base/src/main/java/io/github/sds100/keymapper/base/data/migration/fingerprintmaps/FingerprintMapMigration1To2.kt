@file:Suppress("ClassName")

package io.github.sds100.keymapper.base.data.migration.fingerprintmaps

import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject

/**
 * #621 replace root-only system action ids with their non-root counterpart.
 */
object FingerprintMapMigration1To2 {
    private const val NAME_VERSION = "db_version"
    private const val NAME_ACTION_LIST = "action_list"

    fun migrate(fingerprintMap: JsonObject): JsonObject {
        val actionList by fingerprintMap.byArray(NAME_ACTION_LIST)

        actionList.forEach {
            val data by it.byString("data")

            val newData = when (data) {
                "toggle_wifi_root" -> "toggle_wifi"
                "enable_wifi_root" -> "enable_wifi"
                "disable_wifi_root" -> "disable_wifi"

                "screenshot_root" -> "screenshot"
                "lock_device_no_root" -> "lock_device"

                "show_keyboard_picker_root" -> "show_keyboard_picker"

                else -> data
            }

            it["data"] = newData
        }

        fingerprintMap[NAME_ACTION_LIST] = actionList
        fingerprintMap[NAME_VERSION] = 2

        return fingerprintMap
    }
}
