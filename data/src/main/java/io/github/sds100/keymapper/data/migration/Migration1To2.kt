@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.addAll
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.putAll
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.KeyEventTriggerKeyEntity
import timber.log.Timber

/**
 * Mahoosive update/overhaul from 1.0 to 2.0
 */
object Migration1To2 {
    private const val FLAG_VIBRATE_1 = 4
    private const val FLAG_LONG_PRESS_1 = 1
    private const val FLAG_SHOW_VOLUME_UI_1 = 2

    private const val FLAG_VIBRATE_2 = 1

    private const val MODE_PARALLEL = 0
    private const val MODE_SEQUENCE = 1

    fun migrate(database: SupportSQLiteDatabase) =
        database.apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `new_keymaps` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `trigger` TEXT NOT NULL,
                `action_list` TEXT NOT NULL,
                `constraint_list` TEXT NOT NULL,
                `constraint_mode` INTEGER NOT NULL,
                `flags` INTEGER NOT NULL,
                `folder_name` TEXT,
                `is_enabled` INTEGER NOT NULL)
                """.trimIndent(),
            )

            val query =
                SupportSQLiteQueryBuilder
                    .builder("keymaps")
                    .columns(
                        arrayOf(
                            "id",
                            "trigger_list",
                            "flags",
                            "is_enabled",
                            "action_type",
                            "action_data",
                            "action_extras",
                        ),
                    ).create()

            query(query).apply {
                try {
                    val parser = JsonParser()
                    val gson = Gson()
                    var id = 1

                    while (moveToNext()) {
                        val triggerListOld = parser.parse(getString(1)).asJsonArray
                        val flagsOld = getInt(2)
                        val isEnabledOld = getInt(3)

                        val actionTypeOld =
                            getStringOrNull(4).let {
                                if (it.isNullOrBlank() || it == "NULL") {
                                    null
                                } else {
                                    parser.parse(it).asString
                                }
                            }

                        val actionDataOld = getString(5)

                        val actionExtrasOld =
                            getStringOrNull(6).let {
                                if (it.isNullOrBlank() || it == "NULL") {
                                    null
                                } else {
                                    parser.parse(it).asJsonArray
                                }
                            }

                        val isLongPress = flagsOld.hasFlag(FLAG_LONG_PRESS_1)

                        val triggerListNew = JsonArray()

                        if (triggerListOld.size() == 0) {
                            triggerListNew.add(createTrigger2())
                        }

                        triggerListOld.forEach { trigger ->
                            val newTriggerKeys =
                                trigger["keys"].asJsonArray.map {
                                    val clickType = if (isLongPress) 1 else 0 // long press else short press

                                    createTriggerKey2(
                                        it.asInt,
                                        KeyEventTriggerKeyEntity.DEVICE_ID_ANY_DEVICE,
                                        clickType,
                                    )
                                }

                            val newTriggerKeysJsonArray =
                                JsonArray().apply {
                                    addAll(newTriggerKeys)
                                }

                            val triggerMode =
                                if (newTriggerKeys.size <= 1) {
                                    MODE_SEQUENCE
                                } else {
                                    MODE_PARALLEL
                                }

                            val triggerNew = createTrigger2(newTriggerKeysJsonArray, triggerMode)
                            triggerListNew.add(triggerNew)
                        }

                        val actionTypeNew =
                            when (actionTypeOld) {
                                "KEY", "KEYCODE" -> "KEY_EVENT"
                                else -> actionTypeOld
                            }

                        val actionListNew = JsonArray()

                        if (actionTypeNew != null) {
                            val actionFlags =
                                if (flagsOld.hasFlag(FLAG_SHOW_VOLUME_UI_1)) {
                                    ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI
                                } else {
                                    0
                                }

                            actionListNew.add(
                                createAction2(
                                    actionTypeNew,
                                    actionDataOld,
                                    actionExtrasOld
                                        ?: JsonArray(),
                                    actionFlags,
                                ),
                            )
                        }

                        val flagsNew = if (flagsOld.hasFlag(FLAG_VIBRATE_1)) FLAG_VIBRATE_2 else 0

                        triggerListNew.forEach {
                            execSQL(
                                """
                                INSERT INTO 'new_keymaps' ('id', 'trigger', 'action_list', 'constraint_list', 'constraint_mode', 'flags', 'folder_name', 'is_enabled')
                                VALUES ($id, '${gson.toJson(
                                    it,
                                )}', '${gson.toJson(actionListNew)}', '[]', 1, '$flagsNew', 'NULL', $isEnabledOld)
                                """.trimIndent(),
                            )
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

    private fun createTriggerKey2(
        keyCode: Int,
        deviceId: String,
        clickType: Int,
    ) = JsonObject().apply {
        putAll(
            "keyCode" to keyCode,
            "deviceId" to deviceId,
            "clickType" to clickType,
        )
    }

    private fun createTrigger2(
        keys: JsonArray = JsonArray(),
        mode: Int = MODE_SEQUENCE,
    ) = JsonObject().apply {
        putAll(
            "keys" to keys,
            "extras" to JsonArray(),
            "mode" to mode,
        )
    }

    private fun createAction2(
        type: String,
        data: String,
        extras: JsonArray,
        flags: Int,
    ) = JsonObject().apply {
        putAll(
            "type" to type,
            "data" to data,
            "extras" to extras,
            "flags" to flags,
        )
    }
}
