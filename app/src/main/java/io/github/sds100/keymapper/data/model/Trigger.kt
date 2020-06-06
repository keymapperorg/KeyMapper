package io.github.sds100.keymapper.data.model

import android.view.KeyEvent
import androidx.annotation.IntDef
import io.github.sds100.keymapper.util.isExternalCompat
import io.github.sds100.keymapper.util.result.ExtraNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success

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

        const val DEFAULT_TRIGGER_MODE = SEQUENCE

        const val UNDETERMINED = -1
        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2
    }

    @Mode
    var mode: Int = DEFAULT_TRIGGER_MODE

    class Key(
        val keyCode: Int,
        var deviceId: String = DEVICE_ID_THIS_DEVICE,
        @ClickType var clickType: Int = SHORT_PRESS
    ) {

        companion object {
            //IDS! DON'T CHANGE
            const val DEVICE_ID_THIS_DEVICE = "io.github.sds100.keymapper.THIS_DEVICE"
            const val DEVICE_ID_ANY_DEVICE = "io.github.sds100.keymapper.ANY_DEVICE"

            fun fromKeyEvent(keyEvent: KeyEvent): Key {
                val deviceId = if (keyEvent.device.isExternalCompat) {
                    keyEvent.device.descriptor
                } else {
                    DEVICE_ID_THIS_DEVICE
                }

                return Key(keyEvent.keyCode, deviceId)
            }
        }

        val uniqueId: String
            get() = "$keyCode$clickType$deviceId"

        override fun equals(other: Any?): Boolean {
            return (other as Key).keyCode == keyCode
        }

        override fun hashCode() = keyCode
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

fun sequenceTrigger(vararg key: Trigger.Key) = Trigger(key.toList()).apply { mode = Trigger.SEQUENCE }
fun parallelTrigger(vararg key: Trigger.Key) = Trigger(key.toList()).apply { mode = Trigger.PARALLEL }