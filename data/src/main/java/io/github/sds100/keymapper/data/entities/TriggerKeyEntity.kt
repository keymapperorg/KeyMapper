package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableInt
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.obj
import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializer
import java.util.UUID

sealed class TriggerKeyEntity : Parcelable {
    abstract val clickType: Int
    abstract val uid: String

    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_CLICK_TYPE = "clickType"
        const val NAME_UID = "uid"

        // Click types
        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2

        /**
         * This is required because they are subclasses and Gson doesn't know about their
         * fields otherwise.
         */
        val SERIALIZER: JsonSerializer<TriggerKeyEntity> = jsonSerializer { (key) ->
            when (key) {
                is AssistantTriggerKeyEntity -> Gson().toJsonTree(key)
                is KeyEventTriggerKeyEntity -> Gson().toJsonTree(key)
                is EvdevTriggerKeyEntity -> Gson().toJsonTree(key)
                is FloatingButtonKeyEntity -> Gson().toJsonTree(key)
                is FingerprintTriggerKeyEntity -> Gson().toJsonTree(key)
            }
        }

        val DESERIALIZER: JsonDeserializer<TriggerKeyEntity> =
            jsonDeserializer { (json, _, _) ->
                // nullable because this property was added after backup and restore was released.
                var uid: String? by json.byNullableString(key = NAME_UID)
                uid = uid ?: UUID.randomUUID().toString()

                when {
                    json.obj.has(FloatingButtonKeyEntity.NAME_BUTTON_UID) -> {
                        return@jsonDeserializer deserializeFloatingButtonKey(json, uid!!)
                    }

                    json.obj.has(AssistantTriggerKeyEntity.NAME_ASSISTANT_TYPE) -> {
                        return@jsonDeserializer deserializeAssistantTriggerKey(json, uid!!)
                    }

                    json.obj.has(FingerprintTriggerKeyEntity.NAME_FINGERPRINT_GESTURE_TYPE) -> {
                        return@jsonDeserializer deserializeFingerprintTriggerKey(json, uid!!)
                    }

                    json.obj.has(EvdevTriggerKeyEntity.NAME_DEVICE_PRODUCT) -> {
                        return@jsonDeserializer deserializeEvdevTriggerKey(json, uid!!)
                    }

                    else -> {
                        return@jsonDeserializer deserializeKeyCodeTriggerKey(json, uid!!)
                    }
                }
            }

        private fun deserializeFloatingButtonKey(
            json: JsonElement,
            uid: String,
        ): FloatingButtonKeyEntity {
            val buttonUid by json.byString(FloatingButtonKeyEntity.NAME_BUTTON_UID)
            val clickType by json.byInt(NAME_CLICK_TYPE)

            return FloatingButtonKeyEntity(buttonUid, clickType, uid)
        }

        private fun deserializeAssistantTriggerKey(
            json: JsonElement,
            uid: String,
        ): AssistantTriggerKeyEntity {
            val type by json.byString(AssistantTriggerKeyEntity.NAME_ASSISTANT_TYPE)
            val clickType by json.byInt(NAME_CLICK_TYPE)

            return AssistantTriggerKeyEntity(type, clickType, uid)
        }

        private fun deserializeFingerprintTriggerKey(
            json: JsonElement,
            uid: String,
        ): FingerprintTriggerKeyEntity {
            val type by json.byInt(FingerprintTriggerKeyEntity.NAME_FINGERPRINT_GESTURE_TYPE)
            val clickType by json.byInt(NAME_CLICK_TYPE)

            return FingerprintTriggerKeyEntity(type, clickType, uid)
        }

        private fun deserializeEvdevTriggerKey(
            json: JsonElement,
            uid: String,
        ): EvdevTriggerKeyEntity {
            val keyCode by json.byInt(EvdevTriggerKeyEntity.NAME_KEYCODE)
            val scanCode by json.byInt(EvdevTriggerKeyEntity.NAME_SCANCODE)
            val deviceName by json.byString(EvdevTriggerKeyEntity.NAME_DEVICE_NAME)
            val deviceVendor by json.byInt(EvdevTriggerKeyEntity.NAME_DEVICE_VENDOR)
            val deviceProduct by json.byInt(EvdevTriggerKeyEntity.NAME_DEVICE_PRODUCT)
            val deviceBus by json.byInt(EvdevTriggerKeyEntity.NAME_DEVICE_BUS)
            val clickType by json.byInt(NAME_CLICK_TYPE)
            val flags by json.byNullableInt(EvdevTriggerKeyEntity.NAME_FLAGS)

            return EvdevTriggerKeyEntity(
                keyCode,
                scanCode,
                deviceName,
                deviceBus,
                deviceVendor,
                deviceProduct,
                clickType,
                flags ?: 0,
                uid,
            )
        }

        private fun deserializeKeyCodeTriggerKey(
            json: JsonElement,
            uid: String,
        ): KeyEventTriggerKeyEntity {
            val keyCode by json.byInt(KeyEventTriggerKeyEntity.NAME_KEYCODE)
            val scanCode by json.byInt(KeyEventTriggerKeyEntity.NAME_SCANCODE)
            val deviceId by json.byString(KeyEventTriggerKeyEntity.NAME_DEVICE_ID)
            val deviceName by json.byNullableString(KeyEventTriggerKeyEntity.NAME_DEVICE_NAME)
            val clickType by json.byInt(NAME_CLICK_TYPE)
            val flags by json.byNullableInt(KeyEventTriggerKeyEntity.NAME_FLAGS)

            return KeyEventTriggerKeyEntity(
                keyCode,
                deviceId,
                deviceName,
                clickType,
                flags ?: 0,
                uid,
                scanCode
            )
        }
    }
}
