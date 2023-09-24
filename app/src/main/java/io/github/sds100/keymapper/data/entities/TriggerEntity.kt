package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.annotation.IntDef
import com.github.salomonbrys.kotson.*
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
@Parcelize
data class TriggerEntity(
    @SerializedName(NAME_KEYS)
    val keys: List<KeyEntity> = listOf(),

    @SerializedName(NAME_EXTRAS)
    val extras: List<Extra> = listOf(),

    @Mode
    @SerializedName(NAME_MODE)
    val mode: Int = DEFAULT_TRIGGER_MODE,

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0
) : Parcelable {

    companion object {
        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_KEYS = "keys"
        const val NAME_EXTRAS = "extras"
        const val NAME_MODE = "mode"
        const val NAME_FLAGS = "flags"

        const val PARALLEL = 0
        const val SEQUENCE = 1
        const val UNDEFINED = 2

        //DON'T CHANGE THESE AND THEY MUST BE POWERS OF 2!!
        const val TRIGGER_FLAG_VIBRATE = 1
        const val TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 2
        const val TRIGGER_FLAG_SCREEN_OFF_TRIGGERS = 4
        const val TRIGGER_FLAG_FROM_OTHER_APPS = 8
        const val TRIGGER_FLAG_SHOW_TOAST = 16

        const val DEFAULT_TRIGGER_MODE = UNDEFINED

        const val UNDETERMINED = -1
        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2

        const val EXTRA_SEQUENCE_TRIGGER_TIMEOUT = "extra_sequence_trigger_timeout"
        const val EXTRA_LONG_PRESS_DELAY = "extra_long_press_delay"
        const val EXTRA_DOUBLE_PRESS_DELAY = "extra_double_press_timeout"
        const val EXTRA_VIBRATION_DURATION = "extra_vibration_duration"

        val DESERIALIZER = jsonDeserializer {
            val triggerKeysJsonArray by it.json.byArray(NAME_KEYS)
            val keys = it.context.deserialize<List<KeyEntity>>(triggerKeysJsonArray)

            val extrasJsonArray by it.json.byArray(NAME_EXTRAS)
            val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

            val mode by it.json.byInt(NAME_MODE)

            val flags by it.json.byNullableInt(NAME_FLAGS)

            TriggerEntity(keys, extraList, mode, flags ?: 0)
        }
    }

    @Parcelize
    data class KeyEntity(
        @SerializedName(NAME_KEYCODE)
        val keyCode: Int,
        @SerializedName(NAME_DEVICE_ID)
        val deviceId: String = DEVICE_ID_THIS_DEVICE,

        @SerializedName(NAME_DEVICE_NAME)
        val deviceName: String? = null,

        @ClickType
        @SerializedName(NAME_CLICK_TYPE)
        val clickType: Int = SHORT_PRESS,

        @SerializedName(NAME_FLAGS)
        val flags: Int = 0,

        @SerializedName(NAME_UID)
        val uid: String = UUID.randomUUID().toString()
    ) : Parcelable {

        companion object {
            //DON'T CHANGE THESE. Used for JSON serialization and parsing.
            const val NAME_KEYCODE = "keyCode"
            const val NAME_DEVICE_ID = "deviceId"
            const val NAME_DEVICE_NAME = "deviceName"
            const val NAME_CLICK_TYPE = "clickType"
            const val NAME_FLAGS = "flags"
            const val NAME_UID = "uid"

            //IDS! DON'T CHANGE
            const val DEVICE_ID_THIS_DEVICE = "io.github.sds100.keymapper.THIS_DEVICE"
            const val DEVICE_ID_ANY_DEVICE = "io.github.sds100.keymapper.ANY_DEVICE"

            const val FLAG_DO_NOT_CONSUME_KEY_EVENT = 1

            val DESERIALIZER = jsonDeserializer {
                val keycode by it.json.byInt(NAME_KEYCODE)
                val deviceId by it.json.byString(NAME_DEVICE_ID)
                val deviceName by it.json.byNullableString(NAME_DEVICE_NAME)
                val clickType by it.json.byInt(NAME_CLICK_TYPE)

                //nullable because this property was added after backup and restore was released.
                val flags by it.json.byNullableInt(NAME_FLAGS)
                val uid by it.json.byNullableString(NAME_UID)

                KeyEntity(keycode, deviceId, deviceName, clickType, flags ?: 0, uid ?: UUID.randomUUID().toString())
            }
        }
    }

    @IntDef(value = [PARALLEL, SEQUENCE, UNDEFINED])
    annotation class Mode

    @IntDef(value = [UNDETERMINED, SHORT_PRESS, LONG_PRESS, DOUBLE_PRESS])
    annotation class ClickType
}