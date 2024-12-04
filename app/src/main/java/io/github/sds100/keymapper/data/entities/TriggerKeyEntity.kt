package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableInt
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.util.UUID

sealed class TriggerKeyEntity : Parcelable {
    abstract val clickType: Int
    abstract val uid: String

    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_CLICK_TYPE = "clickType"
        const val NAME_UID = "uid"

        // Click types
        const val UNDETERMINED = -1
        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2

        val DESERIALIZER: JsonDeserializer<TriggerKeyEntity> =
            jsonDeserializer { (json, _, _) ->
                // nullable because this property was added after backup and restore was released.
                var uid: String? by json.byNullableString(key = NAME_UID)
                uid = uid ?: UUID.randomUUID().toString()

                if (json.obj.has(AssistantTriggerKeyEntity.NAME_ASSISTANT_TYPE)) {
                    return@jsonDeserializer deserializeAssistantTriggerKey(json, uid!!)
                } else {
                    return@jsonDeserializer deserializeKeyCodeTriggerKey(json, uid!!)
                }
            }

        private fun deserializeAssistantTriggerKey(
            json: JsonElement,
            uid: String,
        ): AssistantTriggerKeyEntity {
            val type by json.byString(AssistantTriggerKeyEntity.NAME_ASSISTANT_TYPE)
            val clickType by json.byInt(NAME_CLICK_TYPE)

            return AssistantTriggerKeyEntity(type, clickType, uid)
        }

        private fun deserializeKeyCodeTriggerKey(
            json: JsonElement,
            uid: String,
        ): KeyCodeTriggerKeyEntity {
            val keyCode by json.byInt(KeyCodeTriggerKeyEntity.NAME_KEYCODE)
            val deviceId by json.byString(KeyCodeTriggerKeyEntity.NAME_DEVICE_ID)
            val deviceName by json.byNullableString(KeyCodeTriggerKeyEntity.NAME_DEVICE_NAME)
            val clickType by json.byInt(NAME_CLICK_TYPE)
            val flags by json.byNullableInt(KeyCodeTriggerKeyEntity.NAME_FLAGS)

            return KeyCodeTriggerKeyEntity(
                keyCode,
                deviceId,
                deviceName,
                clickType,
                flags ?: 0,
                uid,
            )
        }
    }
}
