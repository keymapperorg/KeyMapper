package io.github.sds100.keymapper.data.model

import android.view.KeyEvent
import androidx.annotation.IntDef
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.InputDeviceUtils
import io.github.sds100.keymapper.util.isExternalCompat
import splitties.resources.appStr

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
class Trigger(var keys: List<Key> = listOf()) {

    companion object {
        const val PARALLEL = 0
        const val SEQUENCE = 1

        const val DEFAULT_TRIGGER_MODE = PARALLEL

        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2
    }

    @Mode
    var mode: Int = DEFAULT_TRIGGER_MODE

    class Key(
        val keyCode: Int,
        deviceId: String,
        @ClickType var clickType: Int = SHORT_PRESS
    ) {

        var deviceId = deviceId
            set(value) {
                deviceName = getDeviceName(value)

                field = value
            }

        /**
         * This needs to be stored in the [Key] model because the name of the device can't be found when it is
         * disconnected.
         */
        var deviceName: String = getDeviceName(deviceId)

        val uniqueId: String
            get() = "$keyCode$deviceName"

        companion object {
            fun fromKeyEvent(keyEvent: KeyEvent): Key {
                val deviceId = if (keyEvent.device.isExternalCompat) {
                    keyEvent.device.descriptor
                } else {
                    DEVICE_ID_THIS_DEVICE
                }

                return Key(keyEvent.keyCode, deviceId)
            }

            private fun getDeviceName(deviceId: String): String {
                return when (deviceId) {
                    DEVICE_ID_THIS_DEVICE -> appStr(R.string.this_device)
                    DEVICE_ID_ANY_DEVICE -> appStr(R.string.any_device)
                    else -> InputDeviceUtils.getName(deviceId)
                }
            }

            //IDS! DON'T CHANGE
            const val DEVICE_ID_THIS_DEVICE = "io.github.sds100.keymapper.THIS_DEVICE"
            const val DEVICE_ID_ANY_DEVICE = "io.github.sds100.keymapper.ANY_DEVICE"
        }

        override fun equals(other: Any?): Boolean {
            return (other as Key).keyCode == keyCode
        }

        override fun hashCode() = keyCode
    }

    @IntDef(value = [PARALLEL, SEQUENCE])
    annotation class Mode

    @IntDef(value = [SHORT_PRESS, LONG_PRESS, DOUBLE_PRESS])
    annotation class ClickType
}