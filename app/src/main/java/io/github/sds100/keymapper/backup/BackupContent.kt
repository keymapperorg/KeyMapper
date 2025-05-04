package io.github.sds100.keymapper.backup

import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntity
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity

data class BackupContent(
    @SerializedName(NAME_DB_VERSION)
    val dbVersion: Int,

    @SerializedName(NAME_APP_VERSION)
    val appVersion: Int?,

    @SerializedName(NAME_KEYMAP_LIST)
    val keyMapList: List<KeyMapEntity>? = null,

    @SerializedName(NAME_DEFAULT_LONG_PRESS_DELAY)
    val defaultLongPressDelay: Int? = null,

    @SerializedName(NAME_DEFAULT_DOUBLE_PRESS_DELAY)
    val defaultDoublePressDelay: Int? = null,

    @SerializedName(NAME_DEFAULT_VIBRATION_DURATION)
    val defaultVibrationDuration: Int? = null,

    @SerializedName(NAME_DEFAULT_REPEAT_DELAY)
    val defaultRepeatDelay: Int? = null,

    @SerializedName(NAME_DEFAULT_REPEAT_RATE)
    val defaultRepeatRate: Int? = null,

    @SerializedName(NAME_DEFAULT_SEQUENCE_TRIGGER_TIMEOUT)
    val defaultSequenceTriggerTimeout: Int? = null,

    @SerializedName(NAME_FLOATING_LAYOUTS)
    val floatingLayouts: List<FloatingLayoutEntity>? = null,

    @SerializedName(NAME_FLOATING_BUTTONS)
    val floatingButtons: List<FloatingButtonEntity>? = null,

    @SerializedName(NAME_GROUPS)
    val groups: List<GroupEntity>? = null,
) {
    companion object {
        const val NAME_DB_VERSION = "keymap_db_version"
        const val NAME_APP_VERSION = "app_version"
        const val NAME_KEYMAP_LIST = "keymap_list"
        const val NAME_DEFAULT_LONG_PRESS_DELAY = "default_long_press_delay"
        const val NAME_DEFAULT_DOUBLE_PRESS_DELAY = "default_double_press_delay"
        const val NAME_DEFAULT_VIBRATION_DURATION = "default_vibration_duration"
        const val NAME_DEFAULT_REPEAT_DELAY = "default_repeat_delay"
        const val NAME_DEFAULT_REPEAT_RATE = "default_repeat_rate"
        const val NAME_DEFAULT_SEQUENCE_TRIGGER_TIMEOUT = "default_sequence_trigger_timeout"
        const val NAME_FLOATING_LAYOUTS = "floating_layouts"
        const val NAME_FLOATING_BUTTONS = "floating_buttons"
        const val NAME_GROUPS = "groups"

        @Deprecated("Device info used to be stored in a database table but they are now stored inside the triggers and actions.")
        const val NAME_DEVICE_INFO = "device_info"

        @Deprecated("Fingerprint maps were merged into key maps in version 3.0.0")
        const val NAME_FINGERPRINT_MAP_LIST = "fingerprint_map_list"
    }
}
