@file:Suppress("ClassName")

package io.github.sds100.keymapper.data.migration

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.set
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.sds100.keymapper.common.utils.firstBlocking

/**
 * Move fingerprint maps from data store into sqlite database and move device info list information
 * into key maps and fingerprint maps.
 */
object Migration11To12 {

    fun migrateDatabase(
        database: SupportSQLiteDatabase,
        fingerprintMapDataStore: DataStore<Preferences>,
    ) {
        val parser = JsonParser()
        val gson = Gson()

        database.execSQL("CREATE TABLE IF NOT EXISTS `fingerprintmaps` (`id` INTEGER NOT NULL, `action_list` TEXT NOT NULL, `constraint_list` TEXT NOT NULL, `constraint_mode` INTEGER NOT NULL, `extras` TEXT NOT NULL, `flags` INTEGER NOT NULL, `is_enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))")

        val legacyFingerprintIdMap = mapOf(
            "swipe_down" to 0,
            "swipe_up" to 1,
            "swipe_left" to 2,
            "swipe_right" to 3,
        )

        val deviceInfoList: JsonArray = sequence {
            val deviceInfoQuery = SupportSQLiteQueryBuilder
                .builder("deviceinfo")
                .columns(arrayOf("descriptor", "name"))
                .create()

            val cursor = database.query(deviceInfoQuery)

            val descriptorColumnIndex = cursor.getColumnIndex("descriptor")
            val nameColumnIndex = cursor.getColumnIndex("name")

            while (cursor.moveToNext()) {
                val descriptor = cursor.getString(descriptorColumnIndex)
                val name = cursor.getString(nameColumnIndex)

                val jsonObject = JsonObject()
                jsonObject["descriptor"] = descriptor
                jsonObject["name"] = name

                yield(jsonObject)
            }
        }.toList().toJsonArray()

        legacyFingerprintIdMap.forEach { (legacyId, newId) ->

            val legacyFingerprintMap =
                fingerprintMapDataStore.data.firstBlocking()[stringPreferencesKey(legacyId)]

            if (legacyFingerprintMap != null) {
                val rootElement = parser.parse(legacyFingerprintMap)

                val oldActionListJson = rootElement["action_list"].asJsonArray
                val newActionListJson = migrateActionList(oldActionListJson, deviceInfoList)
                val sqlActionList = gson.toJson(newActionListJson).convertJsonValueToSqlValue()

                val constraintList =
                    rootElement.convertValueToJson(gson, "constraints").convertJsonValueToSqlValue()
                val constraintMode =
                    rootElement.convertValueToJson(gson, "constraint_mode")
                        .convertJsonValueToSqlValue()
                val extras =
                    rootElement.convertValueToJson(gson, "extras").convertJsonValueToSqlValue()
                val flags =
                    rootElement.convertValueToJson(gson, "flags").convertJsonValueToSqlValue()
                val isEnabled =
                    rootElement.convertValueToJson(gson, "enabled").convertJsonValueToSqlValue()

                database.execSQL("INSERT INTO fingerprintmaps (id, action_list, constraint_list, constraint_mode, extras, flags, is_enabled) VALUES('$newId', '$sqlActionList', '$constraintList', '$constraintMode', '$extras', '$flags', '$isEnabled')")
            }
        }

        val keyMapListQuery = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "trigger", "action_list"))
            .create()

        val cursor = database.query(keyMapListQuery)

        val keyMapIdColumnIndex = cursor.getColumnIndex("id")
        val triggerColumnIndex = cursor.getColumnIndex("trigger")
        val keyMapActionListColumnIndex = cursor.getColumnIndex("action_list")

        while (cursor.moveToNext()) {
            val id = cursor.getLong(keyMapIdColumnIndex)
            val triggerJson = cursor.getString(triggerColumnIndex)
            val triggerJsonObject = parser.parse(triggerJson).asJsonObject

            val actionListJson = cursor.getString(keyMapActionListColumnIndex)
            val actionListJsonArray = parser.parse(actionListJson).asJsonArray

            val newTriggerJson = migrateKeyMapTrigger(triggerJsonObject, deviceInfoList)
            val newActionListJson = migrateActionList(actionListJsonArray, deviceInfoList)

            val sqlTrigger = gson.toJson(newTriggerJson).convertJsonValueToSqlValue()
            val sqlActionList = gson.toJson(newActionListJson).convertJsonValueToSqlValue()

            database.execSQL("UPDATE keymaps SET trigger='$sqlTrigger', action_list='$sqlActionList' WHERE id=$id")
        }

        database.execSQL("DROP TABLE deviceinfo")
    }

    fun migrateFingerprintMap(
        fingerprintMapId: String,
        fingerprintMap: JsonObject,
        deviceInfoList: JsonArray,
    ): JsonObject {
        val legacyFingerprintIdMap = mapOf(
            "swipe_down" to 0,
            "swipe_up" to 1,
            "swipe_left" to 2,
            "swipe_right" to 3,
        )

        val oldActionList = fingerprintMap["action_list"].asJsonArray
        fingerprintMap["action_list"] = migrateActionList(oldActionList, deviceInfoList)
        fingerprintMap["id"] = legacyFingerprintIdMap[fingerprintMapId] ?: -1
        fingerprintMap.remove("db_version")

        return fingerprintMap
    }

    fun migrateKeyMap(keyMap: JsonObject, deviceInfoList: JsonArray): JsonObject {
        val oldActionList = keyMap["actionList"].asJsonArray
        keyMap["actionList"] = migrateActionList(oldActionList, deviceInfoList)

        val oldTrigger = keyMap["trigger"].asJsonObject
        keyMap["trigger"] = migrateKeyMapTrigger(oldTrigger, deviceInfoList)

        return keyMap
    }

    private fun migrateKeyMapTrigger(
        trigger: JsonElement,
        deviceInfoList: JsonArray,
    ): JsonElement {
        val oldTriggerKeys = trigger["keys"].asJsonArray

        val newTriggerKeys = oldTriggerKeys.map { triggerKey ->
            val deviceId = triggerKey["deviceId"].asString

            if (deviceId != "io.github.sds100.keymapper.THIS_DEVICE" ||
                deviceId != "io.github.sds100.keymapper.ANY_DEVICE"
            ) {
                val deviceDescriptor = deviceId

                triggerKey["deviceName"] = deviceInfoList
                    .find { it["descriptor"].asString == deviceDescriptor }
                    ?.get("name")
                    ?.asString
                    ?: ""
            } else {
                triggerKey["deviceName"] = null
            }

            return@map triggerKey
        }

        trigger["keys"] = newTriggerKeys.toJsonArray()

        return trigger
    }

    private fun migrateActionList(
        actionList: JsonArray,
        deviceInfoList: JsonArray,
    ): JsonArray {
        return actionList.map { action ->
            if (action["type"].asString == "KEY_EVENT") {
                val extras = action["extras"].asJsonArray

                val deviceDescriptor = extras
                    .find { it["id"].asString == "extra_device_descriptor" }
                    ?.get("data")?.asString

                val deviceNameExtra = JsonObject().apply {
                    this["id"] = "extra_device_name"
                    this["data"] = deviceInfoList
                        .find { it["descriptor"].asString == deviceDescriptor }
                        ?.get("name")
                        ?.asString
                        ?: ""
                }

                extras.add(deviceNameExtra)

                action["extras"] = extras
            }

            return@map action
        }.toJsonArray()
    }

    private fun String?.convertJsonValueToSqlValue() =
        when {
            this == null -> "NULL"
            this == "true" -> 1
            this == "false" -> 0
            this.toIntOrNull() != null -> this.toInt()
            else -> this
        }

    private fun JsonElement.convertValueToJson(gson: Gson, key: String) = try {
        gson.toJson(this[key])
    } catch (e: NoSuchElementException) {
        null
    }
}
