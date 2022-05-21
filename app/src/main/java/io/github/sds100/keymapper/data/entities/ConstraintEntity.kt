package io.github.sds100.keymapper.data.entities

import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName

/**
 * Created by sds100 on 17/03/2020.
 */

data class ConstraintEntity(
    @SerializedName(NAME_TYPE)
    val type: String,

    @SerializedName(NAME_EXTRAS)
    val extras: List<Extra>
) {

    constructor(type: String, vararg extra: Extra) : this(type, extra.toList())

    companion object {
        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_TYPE = "type"
        const val NAME_EXTRAS = "extras"

        const val MODE_OR = 0
        const val MODE_AND = 1
        const val DEFAULT_MODE = MODE_AND

        //types
        const val APP_FOREGROUND = "constraint_app_foreground"
        const val APP_NOT_FOREGROUND = "constraint_app_not_foreground"
        const val APP_PLAYING_MEDIA = "constraint_app_playing_media"
        const val APP_NOT_PLAYING_MEDIA = "constraint_app_not_playing_media"
        const val MEDIA_PLAYING = "constraint_media_playing"
        const val NO_MEDIA_PLAYING = "constraint_no_media_playing"

        const val BT_DEVICE_CONNECTED = "constraint_bt_device_connected"
        const val BT_DEVICE_DISCONNECTED = "constraint_bt_device_disconnected"

        const val SCREEN_ON = "constraint_screen_on"
        const val SCREEN_OFF = "constraint_screen_off"

        const val ORIENTATION_0 = "constraint_orientation_0"
        const val ORIENTATION_90 = "constraint_orientation_90"
        const val ORIENTATION_180 = "constraint_orientation_180"
        const val ORIENTATION_270 = "constraint_orientation_270"
        const val ORIENTATION_PORTRAIT = "constraint_orientation_portrait"
        const val ORIENTATION_LANDSCAPE = "constraint_orientation_landscape"

        const val FLASHLIGHT_ON = "flashlight_on"
        const val FLASHLIGHT_OFF = "flashlight_off"

        const val WIFI_ON = "wifi_on"
        const val WIFI_OFF = "wifi_off"
        const val WIFI_CONNECTED = "wifi_connected"
        const val WIFI_DISCONNECTED = "wifi_disconnected"

        const val IME_CHOSEN = "ime_chosen"
        const val IME_NOT_CHOSEN = "ime_not_chosen"

        const val DEVICE_IS_LOCKED = "is_locked"
        const val DEVICE_IS_UNLOCKED = "is_unlocked"

        const val IN_PHONE_CALL = "in_phone_call"
        const val NOT_IN_PHONE_CALL = "not_in_phone_call"
        const val PHONE_RINGING = "phone_ringing"

        const val CHARGING = "charging"
        const val DISCHARGING = "discharging"

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_BT_ADDRESS = "extra_bluetooth_device_address"
        const val EXTRA_BT_NAME = "extra_bluetooth_device_name"
        const val EXTRA_FLASHLIGHT_CAMERA_LENS = "extra_flashlight_camera_lens"
        const val EXTRA_SSID = "extra_ssid"
        const val EXTRA_IME_ID = "extra_ime_id"
        const val EXTRA_IME_LABEL = "extra_ime_label"

        val DESERIALIZER = jsonDeserializer {
            val type by it.json.byString(NAME_TYPE)

            val extrasJsonArray by it.json.byArray(NAME_EXTRAS)
            val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

            ConstraintEntity(type, extraList)
        }
    }
}

