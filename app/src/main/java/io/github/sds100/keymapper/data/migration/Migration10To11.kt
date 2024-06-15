@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * #621 replace root-only system action ids with their non-root counterpart.
 */
object Migration10To11 {

    private const val NAME_ACTION_LIST = "actionList"

    fun migrateDatabase(database: SupportSQLiteDatabase) = database.apply {
        val parser = JsonParser()
        val gson = Gson()

        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "action_list"))
            .create()

        query(query).apply {
            while (moveToNext()) {
                val idColumnIndex = getColumnIndex("id")
                val id = getInt(idColumnIndex)

                val actionListJson = getString(getColumnIndex("action_list"))
                val actionListJsonArray = parser.parse(actionListJson).asJsonArray

                val newActionList = migrate(actionListJsonArray).actionList
                val newActionListJson = gson.toJson(newActionList)

                execSQL("UPDATE keymaps SET action_list='$newActionListJson' WHERE id=$id")
            }

            close()
        }
    }

    fun migrateJson(keyMap: JsonObject): JsonObject {
        val oldActionList by keyMap.byArray(NAME_ACTION_LIST)

        keyMap[NAME_ACTION_LIST] = migrate(oldActionList).actionList

        return keyMap
    }

    private fun migrate(actionList: JsonArray): MigrateModel {
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

        return MigrateModel(actionList)
    }

    private data class MigrateModel(val actionList: JsonArray)
}
