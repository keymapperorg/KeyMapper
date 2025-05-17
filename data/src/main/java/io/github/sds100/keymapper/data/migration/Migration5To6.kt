@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonParser



/**
 * move keymap flags to trigger flags
 */
object Migration5To6 {

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger", "flags"))
            .create()

        query(query).apply {
            val parser = JsonParser()
            val gson = Gson()

            while (moveToNext()) {
                val idColumnIndex = getColumnIndex("id")
                val id = getInt(idColumnIndex)

                val keymapFlags = getInt(getColumnIndex("flags"))

                val trigger = parser.parse(getString(getColumnIndex("trigger"))).asJsonObject

                trigger["flags"] = keymapFlags

                execSQL("UPDATE keymaps SET trigger='${gson.toJson(trigger)}', flags=0 WHERE id=$id")
            }

            close()
        }
    }
}
