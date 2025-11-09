package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import java.util.UUID
import kotlinx.parcelize.Parcelize

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
    val extras: List<EntityExtra> = listOf(),

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0,

    @SerializedName(NAME_UID)
    val uid: String = UUID.randomUUID().toString(),

) : Parcelable {
    companion object {

        // DON'T CHANGE THESE IDs!!!!

        /**
         * The KeyEvent meta state is stored as bit flags.
         */
        const val EXTRA_KEY_EVENT_META_STATE = "extra_meta_state"
        const val EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR = "extra_device_descriptor"
        const val EXTRA_KEY_EVENT_DEVICE_NAME = "extra_device_name"

//        const val EXTRA_KEY_EVENT_USE_SHELL = "extra_key_event_use_shell"

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
        const val EXTRA_INTENT_EXTRAS = "extra_intent_extras"
        const val EXTRA_FLASH_STRENGTH = "extra_flash_strength"
        const val EXTRA_HTTP_METHOD = "extra_http_method"
        const val EXTRA_HTTP_URL = "extra_http_url"
        const val EXTRA_HTTP_BODY = "extra_http_body"
        const val EXTRA_HTTP_DESCRIPTION = "extra_http_description"
        const val EXTRA_HTTP_AUTHORIZATION_HEADER = "extra_http_authorization_header"
        const val EXTRA_SMS_MESSAGE = "extra_sms_message"
        const val EXTRA_SHELL_COMMAND_USE_ROOT = "extra_shell_command_use_root"
        const val EXTRA_SHELL_COMMAND_DESCRIPTION = "extra_shell_command_description"
        const val EXTRA_SHELL_COMMAND_TIMEOUT = "extra_shell_command_timeout"

        // Accessibility node extras
        const val EXTRA_ACCESSIBILITY_PACKAGE_NAME = "extra_accessibility_package_name"
        const val EXTRA_ACCESSIBILITY_CONTENT_DESCRIPTION =
            "extra_accessibility_content_description"
        const val EXTRA_ACCESSIBILITY_TEXT = "extra_accessibility_text"
        const val EXTRA_ACCESSIBILITY_TOOLTIP = "extra_accessibility_tooltip"
        const val EXTRA_ACCESSIBILITY_HINT = "extra_accessibility_hint"
        const val EXTRA_ACCESSIBILITY_CLASS_NAME = "extra_accessibility_class_name"
        const val EXTRA_ACCESSIBILITY_VIEW_RESOURCE_ID = "extra_accessibility_view_resource_id"
        const val EXTRA_ACCESSIBILITY_UNIQUE_ID = "extra_accessibility_unique_id"
        const val EXTRA_ACCESSIBILITY_ACTIONS = "extra_accessibility_actions"
        const val EXTRA_ACCESSIBILITY_NODE_ACTION = "extra_accessibility_node_action"

        const val EXTRA_MOVE_CURSOR_TYPE = "extra_move_cursor_type"
        const val CURSOR_TYPE_CHAR = "char"
        const val CURSOR_TYPE_WORD = "word"
        const val CURSOR_TYPE_LINE = "line"
        const val CURSOR_TYPE_PARAGRAPH = "paragraph"
        const val CURSOR_TYPE_PAGE = "page"
        const val CURSOR_DIRECTION_START = "start"
        const val CURSOR_DIRECTION_END = "end"

        const val EXTRA_MOVE_CURSOR_DIRECTION = "extra_move_cursor_direction"

        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
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
        const val ACTION_FLAG_SHELL_COMMAND_USE_ROOT = 16
        const val ACTION_FLAG_SHELL_COMMAND_USE_ADB = 32

        const val EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR = "extra_custom_stop_repeat_behaviour"
        const val EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR = "extra_custom_hold_down_behaviour"
        const val EXTRA_REPEAT_DELAY = "extra_hold_down_until_repeat_delay"
        const val EXTRA_REPEAT_RATE = "extra_repeat_delay"
        const val EXTRA_MULTIPLIER = "extra_multiplier"
        const val EXTRA_DELAY_BEFORE_NEXT_ACTION = "extra_delay_before_next_action"
        const val EXTRA_HOLD_DOWN_DURATION = "extra_hold_down_duration"
        const val EXTRA_REPEAT_LIMIT = "extra_repeat_limit"

        val DESERIALIZER = jsonDeserializer {
            val typeString by it.json.byNullableString(NAME_ACTION_TYPE)
            // If it is an unknown type then do not deserialize
            if (typeString == null) {
                return@jsonDeserializer null
            }

            val type: Type = try {
                Type.valueOf(typeString!!)
            } catch (e: IllegalArgumentException) {
                // If it is an unknown type then do not deserialize
                return@jsonDeserializer null
            }

            val data by it.json.byString(NAME_DATA)

            val extrasJsonArray by it.json.byArray(NAME_EXTRAS)
            val extraList = it.context.deserialize<List<EntityExtra>>(extrasJsonArray) ?: listOf()

            val flags by it.json.byInt(NAME_FLAGS)

            val uid by it.json.byNullableString(NAME_UID)

            ActionEntity(
                type,
                data,
                extraList.toMutableList(),
                flags,
                uid
                    ?: UUID.randomUUID().toString(),
            )
        }
    }

    enum class Type {
        // DONT CHANGE THESE
        APP,
        APP_SHORTCUT,
        KEY_EVENT,
        TEXT_BLOCK,
        URL,
        SYSTEM_ACTION,
        TAP_COORDINATE,
        SWIPE_COORDINATE,
        PINCH_COORDINATE,
        INTENT,
        PHONE_CALL,
        SEND_SMS,
        COMPOSE_SMS,
        SOUND,
        INTERACT_UI_ELEMENT,
        SHELL_COMMAND,
    }

    constructor(
        type: Type,
        data: String,
        extra: EntityExtra,
    ) : this(type, data, listOf(extra))
}
