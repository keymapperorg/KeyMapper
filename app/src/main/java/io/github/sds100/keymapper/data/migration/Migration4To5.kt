@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.putAll
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.entities.ActionEntity
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 25/06/20.
 */

/**
 * #382 feat: unique repeat behaviour for each action
 */
object Migration4To5 {

    private const val OLD_KEYMAP_FLAG_VIBRATE = 1
    private const val OLD_KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST = 2
    private const val OLD_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 4
    private const val OLD_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS = 8
    private const val OLD_KEYMAP_FLAG_REPEAT_ACTIONS = 16

    private const val NEW_KEYMAP_FLAG_VIBRATE = 1
    private const val NEW_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 2
    private const val NEW_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS = 4
    private const val NEW_FLAG_ACTION_SHOW_PERFORMING_TOAST = 2

    private const val EXTRA_HOLD_DOWN_DELAY = "extra_hold_down_until_repeat_delay"
    private const val EXTRA_REPEAT_DELAY = "extra_repeat_delay"

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "action_list", "trigger", "flags"))
            .create()

        query(query).apply {
            val gson = Gson()
            val parser = JsonParser()

            while (moveToNext()) {
                val id = getLong(0)

                val actionList = parser.parse(getString(1)).asJsonArray
                val trigger = parser.parse(getString(2)).asJsonObject

                val flags = getInt(3)
                var newKeymapFlags = 0

                if (flags.hasFlag(OLD_KEYMAP_FLAG_VIBRATE)) {
                    newKeymapFlags = newKeymapFlags.withFlag(NEW_KEYMAP_FLAG_VIBRATE)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST)) {
                    actionList.forEach {
                        val actionFlags = it["flags"].asInt

                        it["flags"] = actionFlags.withFlag(NEW_FLAG_ACTION_SHOW_PERFORMING_TOAST)
                    }
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS)) {
                    newKeymapFlags = newKeymapFlags.withFlag(NEW_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION)) {
                    newKeymapFlags = newKeymapFlags.withFlag(
                        NEW_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION,
                    )
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_REPEAT_ACTIONS)) {
                    val repeatDelay = trigger["extras"].asJsonArray.getExtraData(EXTRA_REPEAT_DELAY)
                    val holdDownDelay = trigger["extras"].asJsonArray.getExtraData(
                        EXTRA_HOLD_DOWN_DELAY,
                    )

                    actionList.forEach {
                        val newFlags = it["flags"].asInt.withFlag(ActionEntity.ACTION_FLAG_REPEAT)
                        val newExtras = it["extras"].asJsonArray

                        if (holdDownDelay != null) {
                            newExtras.putExtra(EXTRA_HOLD_DOWN_DELAY, holdDownDelay)
                        }

                        if (repeatDelay != null) {
                            newExtras.putExtra(EXTRA_REPEAT_DELAY, repeatDelay)
                        }

                        it["flags"] = newFlags
                        it["extras"] = newExtras
                    }
                }

                trigger["extras"].asJsonArray.apply {
                    removeAll {
                        it["id"].asString in arrayOf(
                            EXTRA_REPEAT_DELAY,
                            EXTRA_HOLD_DOWN_DELAY,
                        )
                    }
                }

                execSQL(
                    "UPDATE keymaps SET trigger='${gson.toJson(trigger)}', action_list='${
                        gson.toJson(
                            actionList,
                        )
                    }', flags='$newKeymapFlags' WHERE id=$id",
                )
            }

            close()
        }
    }

    private fun JsonArray.getExtraData(id: String): String? =
        singleOrNull { it["id"].asString == id }?.get("data")?.asString

    private fun JsonArray.putExtra(id: String, data: String) {
        val obj = JsonObject().apply {
            putAll(
                "id" to id,
                "data" to data,
            )
        }

        add(obj)
    }
}
