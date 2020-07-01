@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger

/**
 * Created by sds100 on 25/06/20.
 */

private data class Trigger5(@SerializedName(Trigger.NAME_KEYS)
                            val keys: List<Trigger.Key> = listOf(),

                            @SerializedName(Trigger.NAME_EXTRAS)
                            val extras: List<Extra> = listOf(),

                            @Trigger.Mode
                            @SerializedName(Trigger.NAME_MODE)
                            val mode: Int = Trigger.DEFAULT_TRIGGER_MODE)

object Migration_5_6 {

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger", "flags"))
            .create()

        query(query).apply {
            val gson = Gson()

            while (moveToNext()) {
                val idColumnIndex = getColumnIndex("id")
                val id = getInt(idColumnIndex)

                val keymapFlagsColumnIndex = getColumnIndex("flags")
                val keymapFlags = getInt(keymapFlagsColumnIndex)

                val triggerColumnIndex = getColumnIndex("trigger")
                val trigger = gson.fromJson<Trigger5>(getString(triggerColumnIndex))

                val newTrigger = Trigger(trigger.keys, trigger.extras, trigger.mode, keymapFlags)

                execSQL("UPDATE keymaps SET trigger='${newTrigger.json}', flags=0 WHERE id=$id")
            }

            close()
        }
    }

    val Any.json: String
        get() = Gson().toJson(this)
}