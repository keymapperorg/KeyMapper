package io.github.sds100.keymapper.data.model

import android.view.KeyEvent
import androidx.annotation.IntDef
import io.github.sds100.keymapper.util.InputDeviceUtils
import io.github.sds100.keymapper.util.isExternalCompat

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

    data class Key(
        val keyCode: Int,

        var deviceId: String? = null,

        @ClickType var clickType: Int = SHORT_PRESS
    ) {

        /**
         * This needs to be stored in the [Key] model because the name of the device can't be found when it is
         * disconnected.
         */
        var deviceName: String? = if (deviceId == null) {
            null
        } else {
            InputDeviceUtils.getName(deviceId!!)
        }

        val uniqueId: String
            get() = "$keyCode$deviceName"

        companion object {
            fun fromKeyEvent(keyEvent: KeyEvent): Key {
                val deviceId = if (keyEvent.device.isExternalCompat) {
                    keyEvent.device.descriptor
                } else {
                    null
                }

                return Key(keyEvent.keyCode, deviceId)
            }
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