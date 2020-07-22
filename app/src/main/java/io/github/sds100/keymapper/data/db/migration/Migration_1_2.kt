@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import splitties.bitflags.hasFlag
import timber.log.Timber

/**
 * Created by sds100 on 06/06/20.
 */

private data class Trigger1(val keys: List<Int>)

private enum class ActionType1 {
    APP,
    APP_SHORTCUT,
    KEYCODE,
    KEY,
    TEXT_BLOCK,
    URL,
    SYSTEM_ACTION
}

private data class Extra1(val id: String, val data: String)

private data class Trigger2(val keys: List<Trigger.Key> = listOf(),

                            val extras: List<Extra> = listOf(),

                            @Trigger.Mode
                            val mode: Int = Trigger.SEQUENCE)

object Migration_1_2 {
    private const val FLAG_VIBRATE_1 = 4
    private const val FLAG_LONG_PRESS_1 = 1
    private const val FLAG_SHOW_VOLUME_UI_1 = 2

    private const val FLAG_VIBRATE_2 = 1

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        execSQL("""
                CREATE TABLE IF NOT EXISTS `new_keymaps` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `trigger` TEXT NOT NULL,
                `action_list` TEXT NOT NULL,
                `constraint_list` TEXT NOT NULL,
                `constraint_mode` INTEGER NOT NULL,
                `flags` INTEGER NOT NULL,
                `folder_name` TEXT,
                `is_enabled` INTEGER NOT NULL)
                """.trimIndent())

        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger_list", "flags", "is_enabled", "action_type", "action_data", "action_extras"))
            .create()

        query(query).apply {
            try {
                val gson = Gson()
                fun Any.json() = gson.toJson(this)
                var id = 1

                while (moveToNext()) {
                    val triggerListOld: List<Trigger1> = gson.fromJson(getString(1))
                    val flagsOld = getInt(2)
                    val isEnabledOld = getInt(3)

                    val actionTypeJson = getString(4)
                    val actionTypeOld: ActionType1? = if (actionTypeJson.isNullOrBlank() || actionTypeJson == "NULL") {
                        null
                    } else {
                        gson.fromJson(actionTypeJson)
                    }

                    val actionDataOld = getString(5)

                    val actionExtrasJson = getString(6)
                    val actionExtrasOld: List<Extra1> = if (actionExtrasJson.isNullOrBlank() || actionExtrasJson == "NULL") {
                        listOf()
                    } else {
                        gson.fromJson(actionExtrasJson)
                    }

                    val isLongPress = flagsOld.hasFlag(FLAG_LONG_PRESS_1)

                    val newTriggerJsonList = mutableListOf<String>()

                    if (triggerListOld.isEmpty()) {
                        newTriggerJsonList.add(Trigger2().json())
                    }

                    triggerListOld.forEach { trigger ->
                        val newTriggerKeys = trigger.keys.map {
                            val clickType = if (isLongPress) {
                                Trigger.LONG_PRESS
                            } else {
                                Trigger.SHORT_PRESS
                            }

                            Trigger.Key(it, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType)
                        }

                        val triggerMode = if (newTriggerKeys.size <= 1) {
                            Trigger.SEQUENCE
                        } else {
                            Trigger.PARALLEL
                        }

                        val triggerNew = Trigger2(newTriggerKeys, mode = triggerMode)
                        newTriggerJsonList.add(triggerNew.json())
                    }

                    val actionTypeNew = when (actionTypeOld) {
                        ActionType1.KEY, ActionType1.KEYCODE -> ActionType.KEY_EVENT
                        ActionType1.APP -> ActionType.APP
                        ActionType1.APP_SHORTCUT -> ActionType.APP_SHORTCUT
                        ActionType1.TEXT_BLOCK -> ActionType.TEXT_BLOCK
                        ActionType1.URL -> ActionType.URL
                        ActionType1.SYSTEM_ACTION -> ActionType.SYSTEM_ACTION
                        else -> null
                    }

                    val actionListNew = mutableListOf<Action>()

                    if (actionTypeNew != null) {
                        val actionFlags = if (flagsOld.hasFlag(FLAG_SHOW_VOLUME_UI_1)) {
                            Action.ACTION_FLAG_SHOW_VOLUME_UI
                        } else {
                            0
                        }

                        val actionExtras = actionExtrasOld.map {
                            Extra(it.id, it.data)
                        }.toMutableList()

                        actionListNew.add(Action(actionTypeNew, actionDataOld, actionExtras, actionFlags))
                    }

                    val flagsNew = if (flagsOld.hasFlag(FLAG_VIBRATE_1)) {
                        FLAG_VIBRATE_2
                    } else {
                        0
                    }

                    newTriggerJsonList.forEach {

                        execSQL(
                            """
                            INSERT INTO 'new_keymaps' ('id', 'trigger', 'action_list', 'constraint_list', 'constraint_mode', 'flags', 'folder_name', 'is_enabled')
                            VALUES ($id, '${it}', '${actionListNew.json()}', '[]', 1, '$flagsNew', 'NULL', ${isEnabledOld})
                            """.trimIndent())
                        id++
                    }
                }

            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                close()
            }
        }

        execSQL("DROP TABLE keymaps")
        execSQL("ALTER TABLE new_keymaps RENAME TO keymaps")
        execSQL("CREATE TABLE IF NOT EXISTS `deviceinfo` (`descriptor` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`descriptor`))")
    }
}