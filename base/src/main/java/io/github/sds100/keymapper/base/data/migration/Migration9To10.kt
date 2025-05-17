@file:Suppress("ClassName")

package io.github.sds100.keymapper.base.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byObject
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Move the action option "show performing toast when performing" to a trigger option.
 */
object Migration9To10 {

    private const val FLAG_ACTION_SHOW_PERFORMING_TOAST = 2
    private const val FLAG_TRIGGER_SHOW_TOAST = 16
    private const val TRIGGER_NAME_FLAGS = "flags"
    private const val ACTION_NAME_FLAGS = "flags"

    private const val NAME_TRIGGER = "trigger"
    private const val NAME_ACTION_LIST = "actionList"

    fun migrateDatabase(database: SupportSQLiteDatabase) = database.apply {
        val parser = JsonParser()
        val gson = Gson()

        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger", "action_list"))
            .create()

        query(query).apply {
            while (moveToNext()) {
                val idColumnIndex = getColumnIndex("id")
                val id = getInt(idColumnIndex)

                val actionListJson = getString(getColumnIndex("action_list"))
                val actionListJsonArray = parser.parse(actionListJson).asJsonArray

                val triggerJson = getString(getColumnIndex("trigger"))
                val triggerJsonElement = parser.parse(triggerJson)

                val (newTrigger, newActionList) = migrate(triggerJsonElement, actionListJsonArray)
                val newTriggerJson = gson.toJson(newTrigger)
                val newActionListJson = gson.toJson(newActionList)

                execSQL("UPDATE keymaps SET trigger='$newTriggerJson', action_list='$newActionListJson' WHERE id=$id")
            }

            close()
        }
    }

    fun migrateJson(keyMap: JsonObject): JsonObject {
        val oldTrigger by keyMap.byObject(NAME_TRIGGER)
        val oldActionList by keyMap.byArray(NAME_ACTION_LIST)

        val (newTrigger, newActionList) = migrate(oldTrigger, oldActionList)

        keyMap[NAME_TRIGGER] = newTrigger
        keyMap[NAME_ACTION_LIST] = newActionList

        return keyMap
    }

    private fun migrate(
        trigger: JsonElement,
        actionList: JsonArray,
    ): MigrateModel {
        var showToast = false

        actionList.forEach {
            val flags by it.byInt(ACTION_NAME_FLAGS)

            if (flags.hasFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)) showToast = true

            it[ACTION_NAME_FLAGS] = flags.minusFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)
        }

        if (showToast) {
            val flags by trigger.byInt(TRIGGER_NAME_FLAGS)

            trigger[TRIGGER_NAME_FLAGS] = flags.withFlag(FLAG_TRIGGER_SHOW_TOAST)
        }

        return MigrateModel(trigger, actionList)
    }

    private data class MigrateModel(val trigger: JsonElement, val actionList: JsonArray)
}
