@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlin.collections.set

/**
 * Created by sds100 on 25/06/20.
 */

/**
 * #376 make trigger mode settings always visible.
 * Added UNDEFINED trigger mode if the number of keys is <= 1.
 * Also added migration for the db to support this on older keymaps.
 */
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
            val parser = JsonParser()

            while (moveToNext()) {
                val id = getLong(0)

                val trigger = parser.parse(getString(1)).asJsonObject

                if (trigger["keys"].asJsonArray.size() <= 1) {
                    trigger["mode"] = 2 //undefined mode
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