package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class EvdevTriggerKeyEntity(
    @SerializedName(NAME_KEYCODE)
    val keyCode: Int,

    @SerializedName(NAME_SCANCODE)
    val scanCode: Int,

    @SerializedName(NAME_DEVICE_NAME)
    val deviceName: String,

    @SerializedName(NAME_DEVICE_BUS)
    val deviceBus: Int,

    @SerializedName(NAME_DEVICE_VENDOR)
    val deviceVendor: Int,

    @SerializedName(NAME_DEVICE_PRODUCT)
    val deviceProduct: Int,

    @SerializedName(NAME_CLICK_TYPE)
    override val clickType: Int = SHORT_PRESS,

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0,

    @SerializedName(NAME_UID)
    override val uid: String = UUID.randomUUID().toString()
) : TriggerKeyEntity(),
    Parcelable {

    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_KEYCODE = "keyCode"
        const val NAME_SCANCODE = "scanCode"
        const val NAME_DEVICE_NAME = "deviceName"
        const val NAME_DEVICE_BUS = "deviceBus"
        const val NAME_DEVICE_VENDOR = "deviceVendor"
        const val NAME_DEVICE_PRODUCT = "deviceProduct"
        const val NAME_FLAGS = "flags"

        const val FLAG_DO_NOT_CONSUME_KEY_EVENT = 1
    }
}
