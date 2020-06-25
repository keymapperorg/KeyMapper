package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import io.github.sds100.keymapper.util.isVolumeAction
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 25/06/20.
 */

object Migration_2_3 {

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
                val trigger = gson.fromJson<Trigger>(triggerJson)

                val actionListJson = getString(2)
                val actionList = gson.fromJson<List<Action>>(actionListJson)

                val flags = getInt(3)

                var newFlags = flags

                if (isRepeatable(trigger, actionList)) {
                    newFlags = flags.withFlag(KeyMap.KEYMAP_FLAG_REPEAT_ACTIONS)
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

    private fun isRepeatable(trigger: Trigger, actionList: List<Action>): Boolean {
        return actionList.any { it.type in arrayOf(ActionType.KEY_EVENT, ActionType.TEXT_BLOCK) || it.isVolumeAction }
            && KeymapDetectionDelegate.performActionOnDown(trigger.keys, trigger.mode)
    }
}