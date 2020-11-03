@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.model.Action
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag

/**
 * Created by sds100 on 25/06/20.
 */

/**
 * If an action has both the Repeat and Hold Down flags then remove the Hold Down flag and assume that the user wants
 * the Repeat flag.
 */
object Migration_7_8 {

    private const val ACTION_FLAG_REPEAT = 4
    private const val ACTION_FLAG_HOLD_DOWN = 8

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "action_list"))
            .create()

        query(query).apply {
            val gson = GsonBuilder().registerTypeAdapter(Action.DESERIALIZER).create()

            while (moveToNext()) {
                val idColumnIndex = getColumnIndex("id")
                val id = getInt(idColumnIndex)

                val actionListColumnIndex = getColumnIndex("action_list")

                val actionList = gson.fromJson<List<Action>>(getString(actionListColumnIndex))

                val newActionList = actionList.map {
                    if (it.flags.hasFlag(ACTION_FLAG_REPEAT) && it.flags.hasFlag(ACTION_FLAG_HOLD_DOWN)) {
                        return@map it.clone(flags = it.flags.minusFlag(ACTION_FLAG_HOLD_DOWN))
                    }

                    it
                }

                execSQL("UPDATE keymaps SET action_list='${newActionList.json}' WHERE id=$id")
            }

            close()
        }
    }

    val Any.json: String
        get() = Gson().toJson(this)
}