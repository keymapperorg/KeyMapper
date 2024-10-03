package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableInt
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class TriggerKeyEntity(
    @SerializedName(NAME_KEYCODE)
    val keyCode: Int,
    @SerializedName(NAME_DEVICE_ID)
    val deviceId: String = DEVICE_ID_THIS_DEVICE,

    @SerializedName(NAME_DEVICE_NAME)
    val deviceName: String? = null,

    @TriggerEntity.ClickType
    @SerializedName(NAME_CLICK_TYPE)
    val clickType: Int = TriggerEntity.SHORT_PRESS,

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0,

    @SerializedName(NAME_UID)
    val uid: String = UUID.randomUUID().toString(),
) : Parcelable {

    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_KEYCODE = "keyCode"
        const val NAME_DEVICE_ID = "deviceId"
        const val NAME_DEVICE_NAME = "deviceName"
        const val NAME_CLICK_TYPE = "clickType"
        const val NAME_FLAGS = "flags"
        const val NAME_UID = "uid"

        // IDS! DON'T CHANGE
        const val DEVICE_ID_THIS_DEVICE = "io.github.sds100.keymapper.THIS_DEVICE"
        const val DEVICE_ID_ANY_DEVICE = "io.github.sds100.keymapper.ANY_DEVICE"

        const val FLAG_DO_NOT_CONSUME_KEY_EVENT = 1

        val DESERIALIZER = jsonDeserializer {
            val keycode by it.json.byInt(NAME_KEYCODE)
            val deviceId by it.json.byString(NAME_DEVICE_ID)
            val deviceName by it.json.byNullableString(NAME_DEVICE_NAME)
            val clickType by it.json.byInt(NAME_CLICK_TYPE)

            // nullable because this property was added after backup and restore was released.
            val flags by it.json.byNullableInt(NAME_FLAGS)
            val uid by it.json.byNullableString(NAME_UID)

            TriggerKeyEntity(
                keycode,
                deviceId,
                deviceName,
                clickType,
                flags ?: 0,
                uid ?: UUID.randomUUID().toString(),
            )
        }
    }
}
