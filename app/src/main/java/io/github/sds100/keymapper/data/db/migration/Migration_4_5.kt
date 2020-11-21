@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 25/06/20.
 */


private data class Trigger4(val keys: List<Trigger.Key> = listOf(),

                            val extras: List<Extra> = listOf(),

                            @Trigger.Mode
                            val mode: Int = Trigger.DEFAULT_TRIGGER_MODE)

object Migration_4_5 {

    private const val OLD_KEYMAP_FLAG_VIBRATE = 1
    private const val OLD_KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST = 2
    private const val OLD_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 4
    private const val OLD_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS = 8
    private const val OLD_KEYMAP_FLAG_REPEAT_ACTIONS = 16

    private const val NEW_KEYMAP_FLAG_VIBRATE = 1
    private const val NEW_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 2
    private const val NEW_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS = 4

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "action_list", "trigger", "flags"))
            .create()

        query(query).apply {
            val gson = Gson()

            while (moveToNext()) {
                val id = getLong(0)

                val actionListJson = getString(1)
                var actionList = gson.fromJson<List<Action>>(actionListJson)

                val triggerJson = getString(2)
                val trigger = gson.fromJson<Trigger4>(triggerJson)

                val flags = getInt(3)
                var newKeymapFlags = 0

                if (flags.hasFlag(OLD_KEYMAP_FLAG_VIBRATE)) {
                    newKeymapFlags = newKeymapFlags.withFlag(NEW_KEYMAP_FLAG_VIBRATE)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST)) {
                    actionList = actionList.map {
                        it.copy(flags = it.flags.withFlag(Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST))
                    }
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS)) {
                    newKeymapFlags = newKeymapFlags.withFlag(NEW_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION)) {
                    newKeymapFlags = newKeymapFlags.withFlag(NEW_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_REPEAT_ACTIONS)) {
                    val repeatDelay = trigger.extras.getData(Action.EXTRA_REPEAT_RATE).valueOrNull()
                    val holdDownDelay = trigger.extras.getData(Action.EXTRA_REPEAT_DELAY).valueOrNull()

                    actionList = actionList.map {
                        val newFlags = it.flags.withFlag(Action.ACTION_FLAG_REPEAT)
                        var newExtras = it.extras

                        if (holdDownDelay != null) {
                            newExtras = newExtras.putExtraData(Action.EXTRA_REPEAT_DELAY, holdDownDelay)
                        }

                        if (repeatDelay != null) {
                            newExtras = newExtras.putExtraData(Action.EXTRA_REPEAT_RATE, repeatDelay)
                        }

                        it.copy(flags = newFlags, extras = newExtras)
                    }
                }

                val newTriggerExtras = trigger.extras.toMutableList().apply {
                    removeAll { it.id == Action.EXTRA_REPEAT_RATE || it.id == Action.EXTRA_REPEAT_DELAY }
                }

                val newTrigger = Trigger4(trigger.keys, newTriggerExtras.toList(), mode = trigger.mode)

                execSQL("UPDATE keymaps SET trigger='${newTrigger.json}', action_list='${actionList.json}', flags='$newKeymapFlags' WHERE id=$id")
            }

            close()
        }
    }

    private val Any.json: String
        get() = Gson().toJson(this)
}