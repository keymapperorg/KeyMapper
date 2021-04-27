package io.github.sds100.keymapper.data.migration.fingerprintmaps

import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 22/01/21.
 */

/**
 * Move the action option "show performing toast when performing" to a trigger option.
 */
object Migration_0_1 {
    private const val NAME_VERSION = "db_version"
    private const val NAME_ACTION_LIST = "action_list"
    private const val FLAG_ACTION_SHOW_PERFORMING_TOAST = 2
    private const val FLAG_SHOW_TOAST = 2
    private const val TRIGGER_NAME_FLAGS = "flags"
    private const val ACTION_NAME_FLAGS = "flags"

    fun migrate(gson: Gson, json: String): String {
        val parser = JsonParser()

        val rootElement = parser.parse(json)

        val actionListJsonArray by rootElement.byArray(NAME_ACTION_LIST)

        var showToast = false

        actionListJsonArray.forEach {
            val flags by it.byInt(ACTION_NAME_FLAGS)

            if (flags.hasFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)) showToast = true

            it[ACTION_NAME_FLAGS] = flags.minusFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)
        }

        rootElement[NAME_ACTION_LIST] = actionListJsonArray

        if (showToast) {
            val flags by rootElement.byInt(TRIGGER_NAME_FLAGS)

            rootElement[TRIGGER_NAME_FLAGS] = flags.withFlag(FLAG_SHOW_TOAST)
        }

        rootElement[NAME_VERSION] = 1
        return gson.toJson(rootElement)
    }
}