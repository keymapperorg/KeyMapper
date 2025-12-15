package io.github.sds100.keymapper.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class GrabDeviceRequest(
    val device: EvdevDeviceInfo,
    /**
     * The Android key codes to add support for from this device.
     *
     * One can request the duplicated uinput device to support extra key codes that the
     * original device doesn't support. This is needed when a key map for this device
     * inputs a key code that isn't supported by the original evdev device.
     */
    val extraKeyCodes: IntArray,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrabDeviceRequest

        if (device != other.device) return false
        if (!extraKeyCodes.contentEquals(other.extraKeyCodes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + extraKeyCodes.contentHashCode()
        return result
    }
}
