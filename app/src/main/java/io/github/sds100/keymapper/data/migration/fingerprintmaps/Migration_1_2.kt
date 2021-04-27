@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration.fingerprintmaps

import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * #621 replace root-only system action ids with their non-root counterpart.
 */
object Migration_1_2 {
    private const val NAME_VERSION = "db_version"
    private const val NAME_ACTION_LIST = "action_list"

    fun migrate(gson: Gson, json: String): String {
        val parser = JsonParser()

        val root = parser.parse(json)

        val actionList by root.byArray(NAME_ACTION_LIST)

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

        root[NAME_ACTION_LIST] = actionList
        root[NAME_VERSION] = 2

        return gson.toJson(root)
    }
}