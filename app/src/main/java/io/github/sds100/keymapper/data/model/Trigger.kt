package io.github.sds100.keymapper.data.model

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.IntDef
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.InputDeviceUtils
import io.github.sds100.keymapper.util.isExternalCompat
import io.github.sds100.keymapper.util.result.ExtraNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import io.github.sds100.keymapper.util.result.handle
import splitties.resources.appStr
import splitties.resources.str

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
class Trigger(var keys: List<Key> = listOf(), val extras: List<Extra> = listOf()) {

    companion object {
        const val PARALLEL = 0
        const val SEQUENCE = 1

        const val DEFAULT_TRIGGER_MODE = PARALLEL

        const val UNDETERMINED = -1
        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2
    }

    @Mode
    var mode: Int = DEFAULT_TRIGGER_MODE

    data class Key(
        val keyCode: Int,
        var device: DeviceInfo,
        @ClickType var clickType: Int = SHORT_PRESS
    ) {

        companion object {
            //IDS! DON'T CHANGE
            const val DEVICE_ID_THIS_DEVICE = "io.github.sds100.keymapper.THIS_DEVICE"
            const val DEVICE_ID_ANY_DEVICE = "io.github.sds100.keymapper.ANY_DEVICE"

            fun fromKeyEvent(ctx: Context, keyEvent: KeyEvent): Trigger.Key {
                val deviceId = if (keyEvent.device.isExternalCompat) {
                    keyEvent.device.descriptor
                } else {
                    Trigger.Key.DEVICE_ID_THIS_DEVICE
                }

                return Trigger.Key(keyEvent.keyCode, Trigger.DeviceInfo(ctx, deviceId))
            }

        }

        val uniqueId: String
            get() = "$keyCode$clickType${device.name}"

        override fun equals(other: Any?): Boolean {
            return (other as Key).keyCode == keyCode
        }

        override fun hashCode() = keyCode
    }

    data class DeviceInfo(val descriptor: String, val name: String) {
        companion object {
            fun getDeviceName(ctx: Context, descriptor: String): String = when (descriptor) {
                Key.DEVICE_ID_THIS_DEVICE -> appStr(R.string.this_device)
                Key.DEVICE_ID_ANY_DEVICE -> appStr(R.string.any_device)
                else -> InputDeviceUtils.getName(descriptor).handle(
                    onSuccess = { it },
                    onFailure = { ctx.str(R.string.trigger_device_name_not_found) }
                )
            }

        }

        constructor(ctx: Context, descriptor: String) : this(descriptor, getDeviceName(ctx, descriptor))
    }

    fun getExtraData(extraId: String): Result<String> {

        return extras.find { it.id == extraId }.let {
            it ?: return@let ExtraNotFound(extraId)

            Success(it.data)
        }
    }

    @IntDef(value = [PARALLEL, SEQUENCE])
    annotation class Mode

    @IntDef(value = [UNDETERMINED, SHORT_PRESS, LONG_PRESS, DOUBLE_PRESS])
    annotation class ClickType
}