@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonParser
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Move the action option "show performing toast when performing" to a trigger option.
 */
object Migration_9_10 {

    private const val FLAG_ACTION_SHOW_PERFORMING_TOAST = 2
    private const val FLAG_TRIGGER_SHOW_TOAST = 16
    private const val TRIGGER_NAME_FLAGS = "flags"
    private const val ACTION_NAME_FLAGS = "flags"

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
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

                var showToast = false

                actionListJsonArray.forEach {
                    val flags by it.byInt(ACTION_NAME_FLAGS)

                    if (flags.hasFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)) showToast = true

                    it[ACTION_NAME_FLAGS] = flags.minusFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)
                }

                val newActionListJson = gson.toJson(actionListJsonArray)

                if (showToast) {
                    val triggerJson = getString(getColumnIndex("trigger"))
                    val rootTriggerElement = parser.parse(triggerJson)
                    val flags by rootTriggerElement.byInt(TRIGGER_NAME_FLAGS)

                    rootTriggerElement[TRIGGER_NAME_FLAGS] = flags.withFlag(FLAG_TRIGGER_SHOW_TOAST)

                    val newTriggerJson = gson.toJson(rootTriggerElement)

                    execSQL("UPDATE keymaps SET trigger='$newTriggerJson', action_list='$newActionListJson' WHERE id=$id")
                }
            }

            close()
        }
    }
}