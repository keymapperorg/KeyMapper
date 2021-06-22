package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.*
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.entities.ActionEntity.Type
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [data] The information required to perform the action. E.g if the type is [Type.APP],
 * the data will be the package name of the application
 *
 * Different Types of actions:
 * - Apps
 * - App shortcuts
 * - Keycode
 * - Key
 * - Insert a block of text
 * - System actions/settings
 */
@Parcelize
data class ActionEntity(
    @SerializedName(NAME_ACTION_TYPE)
    val type: Type,

    /**
     * How each action type saves data:
     *
     * - Apps: package name
     * - App shortcuts: the intent for the shortcut as a parsed URI
     * - Key Event: the keycode. Any extra information is stored in [extras]
     * - Block of text: text to insert
     * - System action: the system action id
     * - Tap coordinate: comma separated x and y values
     * - Intent: the Intent parsed as a URI
     */
    @SerializedName(NAME_DATA)
    val data: String,

    @SerializedName(NAME_EXTRAS)
    val extras: List<Extra> = listOf(),

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0,

    @SerializedName(NAME_UID)
    val uid: String = UUID.randomUUID().toString()

) : Parcelable {
    companion object {

        //DON'T CHANGE THESE IDs!!!!

        /**
         * The KeyEvent meta state is stored as bit flags.
         */
        const val EXTRA_KEY_EVENT_META_STATE = "extra_meta_state"
        const val EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR = "extra_device_descriptor"
        const val EXTRA_KEY_EVENT_DEVICE_NAME = "extra_device_name"
        const val EXTRA_KEY_EVENT_USE_SHELL = "extra_key_event_use_shell"

        const val EXTRA_IME_ID = "extra_ime_id"
        const val EXTRA_IME_NAME = "extra_ime_name"

        const val EXTRA_SHORTCUT_TITLE = "extra_title"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_STREAM_TYPE = "extra_stream_type"
        const val EXTRA_LENS = "extra_flash"
        const val EXTRA_RINGER_MODE = "extra_ringer_mode"
        const val EXTRA_DND_MODE = "extra_do_not_disturb_mode"
        const val EXTRA_ORIENTATIONS = "extra_orientations"
        const val EXTRA_COORDINATE_DESCRIPTION = "extra_coordinate_description"
        const val EXTRA_INTENT_TARGET = "extra_intent_target"
        const val EXTRA_INTENT_DESCRIPTION = "extra_intent_description"
        const val EXTRA_SOUND_FILE_DESCRIPTION = "extra_sound_file_description"

        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ACTION_TYPE = "type"
        const val NAME_DATA = "data"
        const val NAME_EXTRAS = "extras"
        const val NAME_FLAGS = "flags"
        const val NAME_UID = "uid"

        const val STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN = 0
        const val STOP_REPEAT_BEHAVIOUR_LIMIT_REACHED = 1

        const val STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN = 0

        const val ACTION_FLAG_SHOW_VOLUME_UI = 1
        const val ACTION_FLAG_REPEAT = 4
        const val ACTION_FLAG_HOLD_DOWN = 8

        const val EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR = "extra_custom_stop_repeat_behaviour"
        const val EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR = "extra_custom_hold_down_behaviour"
        const val EXTRA_REPEAT_DELAY = "extra_hold_down_until_repeat_delay"
        const val EXTRA_REPEAT_RATE = "extra_repeat_delay"
        const val EXTRA_MULTIPLIER = "extra_multiplier"
        const val EXTRA_DELAY_BEFORE_NEXT_ACTION = "extra_delay_before_next_action"
        const val EXTRA_HOLD_DOWN_DURATION = "extra_hold_down_duration"
        const val EXTRA_REPEAT_LIMIT = "extra_repeat_limit"

        val DESERIALIZER = jsonDeserializer {
            val typeString by it.json.byString(NAME_ACTION_TYPE)
            val type = Type.valueOf(typeString)

            val data by it.json.byString(NAME_DATA)

            val extrasJsonArray by it.json.byArray(NAME_EXTRAS)
            val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

            val flags by it.json.byInt(NAME_FLAGS)

            val uid by it.json.byNullableString(NAME_UID)

            ActionEntity(
                type, data, extraList.toMutableList(), flags, uid
                ?: UUID.randomUUID().toString()
            )
        }
    }

    enum class Type {
        //DONT CHANGE THESE
        APP, APP_SHORTCUT, KEY_EVENT, TEXT_BLOCK, URL, SYSTEM_ACTION, TAP_COORDINATE, INTENT, PHONE_CALL, SOUND
    }

    constructor(
        type: Type, data: String, extra: Extra
    ) : this(type, data, listOf(extra))
}