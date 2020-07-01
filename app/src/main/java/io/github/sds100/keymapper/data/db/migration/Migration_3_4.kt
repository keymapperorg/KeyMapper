@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger

/**
 * Created by sds100 on 25/06/20.
 */

private data class Trigger3(val keys: List<Trigger.Key> = listOf(),

                            val extras: List<Extra> = listOf(),

                            @Trigger.Mode
                            val mode: Int = Trigger.UNDEFINED)


object Migration_3_4 {

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger"))
            .create()

        //maps the new trigger mode to each keymap id
        val newTriggerMap = mutableMapOf<Long, String>()

        query(query).apply {
            val gson = Gson()

            while (moveToNext()) {
                val id = getLong(0)

                val triggerJson = getString(1)
                var trigger = gson.fromJson<Trigger3>(triggerJson)

                if (trigger.keys.size <= 1) {
                    trigger = Trigger3(trigger.keys, trigger.extras, mode = Trigger.UNDEFINED)
                }

                newTriggerMap[id] = gson.toJson(trigger)
            }

            close()
        }

        newTriggerMap.entries.forEach {
            val id = it.key
            val trigger = it.value

            execSQL("UPDATE keymaps SET trigger='$trigger' WHERE id=$id")
        }
    }
}