@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration.keymaps

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.SystemAction
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 25/06/20.
 */


object Migration_2_3 {

    private data class Trigger2(val keys: List<Trigger.Key> = listOf(),

                                val extras: List<Extra> = listOf(),

                                @Trigger.Mode
                                val mode: Int = Trigger.SEQUENCE)

    private val KEYMAP_FLAG_REPEAT_ACTIONS = 16

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger", "action_list", "flags"))
            .create()

        //maps the new flags to each keymap id
        val newFlagsMap = mutableMapOf<Long, Int>()

        query(query).apply {
            val gson = Gson()

            while (moveToNext()) {
                val id = getLong(0)

                val triggerJson = getString(1)
                val trigger = gson.fromJson<Trigger2>(triggerJson)

                val actionListJson = getString(2)
                val actionList = gson.fromJson<List<Action>>(actionListJson)

                val flags = getInt(3)

                var newFlags = flags

                if (isRepeatable(trigger, actionList)) {
                    newFlags = flags.withFlag(KEYMAP_FLAG_REPEAT_ACTIONS)
                }

                newFlagsMap[id] = newFlags
            }

            close()
        }

        newFlagsMap.entries.forEach {
            val id = it.key
            val flags = it.value

            execSQL("UPDATE keymaps SET flags=$flags WHERE id=$id")
        }
    }

    private fun isRepeatable(trigger: Trigger2, actionList: List<Action>): Boolean {
        return actionList.any {
            it.type in arrayOf(ActionType.KEY_EVENT, ActionType.TEXT_BLOCK) ||
                listOf(
                    SystemAction.VOLUME_DECREASE_STREAM,
                    SystemAction.VOLUME_INCREASE_STREAM,
                    SystemAction.VOLUME_DOWN,
                    SystemAction.VOLUME_UP,
                    SystemAction.VOLUME_MUTE,
                    SystemAction.VOLUME_TOGGLE_MUTE,
                    SystemAction.VOLUME_UNMUTE
                ).contains(it.data)
        }
            && KeymapDetectionDelegate.performActionOnDown(trigger.keys, trigger.mode)
    }
}