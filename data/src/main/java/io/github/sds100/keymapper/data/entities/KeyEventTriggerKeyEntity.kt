package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class KeyEventTriggerKeyEntity(
    @SerializedName(NAME_KEYCODE)
    val keyCode: Int,
    @SerializedName(NAME_DEVICE_ID)
    val deviceId: String = DEVICE_ID_THIS_DEVICE,
    @SerializedName(NAME_DEVICE_NAME)
    val deviceName: String? = null,
    @SerializedName(NAME_CLICK_TYPE)
    override val clickType: Int = SHORT_PRESS,
    @SerializedName(NAME_FLAGS)
    val flags: Int = 0,
    @SerializedName(NAME_UID)
    override val uid: String = UUID.randomUUID().toString(),
    @SerializedName(NAME_SCANCODE)
    val scanCode: Int? = null,
) : TriggerKeyEntity(),
    Parcelable {
    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_KEYCODE = "keyCode"
        const val NAME_SCANCODE = "scanCode"
        const val NAME_DEVICE_ID = "deviceId"
        const val NAME_DEVICE_NAME = "deviceName"
        const val NAME_FLAGS = "flags"

        // IDS! DON'T CHANGE
        const val DEVICE_ID_THIS_DEVICE = "io.github.sds100.keymapper.THIS_DEVICE"
        const val DEVICE_ID_ANY_DEVICE = "io.github.sds100.keymapper.ANY_DEVICE"

        const val FLAG_DO_NOT_CONSUME_KEY_EVENT = 1
        const val FLAG_DETECTION_SOURCE_INPUT_METHOD = 2
        const val FLAG_DETECT_WITH_SCAN_CODE = 4
    }
}
