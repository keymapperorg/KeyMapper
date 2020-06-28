@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.putExtraData
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 25/06/20.
 */

object Migration_4_5 {

    private const val OLD_KEYMAP_FLAG_VIBRATE = 1
    private const val OLD_KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST = 2
    private const val OLD_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 4
    private const val OLD_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS = 8
    private const val OLD_KEYMAP_FLAG_REPEAT_ACTIONS = 16

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
                val trigger = gson.fromJson<Trigger>(triggerJson)

                val flags = getInt(3)
                var newFlags = 0

                if (flags.hasFlag(OLD_KEYMAP_FLAG_VIBRATE)) {
                    newFlags = newFlags.withFlag(KeyMap.KEYMAP_FLAG_VIBRATE)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST)) {
                    actionList = actionList.map {
                        it.clone(flags = it.flags.withFlag(Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST))
                    }
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_SCREEN_OFF_TRIGGERS)) {
                    newFlags = newFlags.withFlag(KeyMap.KEYMAP_FLAG_SCREEN_OFF_TRIGGERS)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION)) {
                    newFlags = newFlags.withFlag(KeyMap.KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
                }

                if (flags.hasFlag(OLD_KEYMAP_FLAG_REPEAT_ACTIONS)) {
                    val repeatDelay = trigger.getExtraData(Action.EXTRA_REPEAT_DELAY).valueOrNull()
                    val holdDownDelay = trigger.getExtraData(Action.EXTRA_HOLD_DOWN_DELAY).valueOrNull()

                    actionList = actionList.map {
                        val newFlags = it.flags.withFlag(Action.ACTION_FLAG_REPEAT)
                        var newExtras = it.extras

                        if (holdDownDelay != null) {
                            newExtras = it.extras.putExtraData(Action.EXTRA_HOLD_DOWN_DELAY, holdDownDelay)
                        }

                        if (repeatDelay != null) {
                            newExtras = it.extras.putExtraData(Action.EXTRA_REPEAT_DELAY, repeatDelay)
                        }

                        it.clone(flags = newFlags, extras = newExtras)
                    }
                }

                val newTriggerExtras = trigger.extras.toMutableList().apply {
                    removeAll { it.id == Action.EXTRA_REPEAT_DELAY || it.id == Action.EXTRA_HOLD_DOWN_DELAY }
                }

                val newTrigger = Trigger(trigger.keys, newTriggerExtras.toList(), mode = trigger.mode)

                execSQL("UPDATE keymaps SET trigger='${newTrigger.json}', action_list='${actionList.json}', flags='$newFlags' WHERE id=$id")
            }

            close()
        }
    }

    val Any.json: String
        get() = Gson().toJson(this)
}