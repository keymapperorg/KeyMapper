@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.get
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.sds100.keymapper.common.utils.withFlag



/**
 * #379 feat: add option to repeat for all types of actions
 */
object Migration2To3 {

    private const val KEYMAP_FLAG_REPEAT_ACTIONS = 16

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger", "action_list", "flags"))
            .create()

        val parser = JsonParser()

        // maps the new flags to each keymap id
        val newFlagsMap = mutableMapOf<Long, Int>()

        query(query).apply {
            while (moveToNext()) {
                val id = getLong(0)

                val trigger = parser.parse(getString(1)).asJsonObject
                val actionList = parser.parse(getString(2)).asJsonArray
                val flags = getInt(3)

                var newFlags = flags

                if (isRepeatable(trigger, actionList)) {
                    newFlags = flags.withFlag(KEYMAP_FLAG_REPEAT_ACTIONS)
                }

                newFlagsMap[id] = newFlags
            }

            close()
        }

        newFlagsMap.entries.forEach {
            val id = it.key
            val flags = it.value

            execSQL("UPDATE keymaps SET flags=$flags WHERE id=$id")
        }
    }

    private fun isRepeatable(trigger: JsonObject, actionList: JsonArray): Boolean = actionList.any {
        it["type"].asString in arrayOf("KEY_EVENT", "TEXT_BLOCK") ||
            it["data"].asString in arrayOf(
                "volume_decrease_stream",
                "volume_increase_stream",
                "volume_down",
                "volume_up",
                "volume_mute",
                "volume_toggle_mute",
                "volume_unmute",
            )
    } &&
        performActionOnDown(trigger["keys"].asJsonArray, trigger["mode"].asInt)

    private fun performActionOnDown(triggerKeys: JsonArray, triggerMode: Int): Boolean {
        return (triggerKeys.size() == 1 && triggerKeys[0]["clickType"].asInt != 2) ||
            // 2 = double press
            triggerMode == 0 // parallel mode
    }
}
