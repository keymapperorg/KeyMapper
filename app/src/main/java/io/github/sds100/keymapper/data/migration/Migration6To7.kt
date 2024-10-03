@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 25/06/20.
 */

object Migration6To7 {

    private const val TRIGGER_FLAG_DONT_OVERRIDE_DEFAULT_ACTION = 8

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger", "flags"))
            .create()

        query(query).apply {
            val gson = GsonBuilder().registerTypeAdapter(TriggerEntity.DESERIALIZER).create()

            while (moveToNext()) {
                val idColumnIndex = getColumnIndex("id")
                val id = getInt(idColumnIndex)

                val triggerColumnIndex = getColumnIndex("trigger")

                val trigger = gson.fromJson<TriggerEntity>(getString(triggerColumnIndex))

                val newTriggerKeys = trigger.keys.map {
                    if (trigger.flags.hasFlag(TRIGGER_FLAG_DONT_OVERRIDE_DEFAULT_ACTION)) {
                        it.copy(flags = it.flags.withFlag(TriggerKeyEntity.FLAG_DO_NOT_CONSUME_KEY_EVENT))
                    } else {
                        it
                    }
                }

                val newTriggerFlags = trigger.flags.minusFlag(
                    TRIGGER_FLAG_DONT_OVERRIDE_DEFAULT_ACTION,
                )
                val newTrigger = trigger.copy(keys = newTriggerKeys, flags = newTriggerFlags)

                execSQL("UPDATE keymaps SET trigger='${newTrigger.json}', flags=0 WHERE id=$id")
            }

            close()
        }
    }

    val Any.json: String
        get() = Gson().toJson(this)
}
