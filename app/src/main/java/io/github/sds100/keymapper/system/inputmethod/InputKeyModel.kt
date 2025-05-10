package io.github.sds100.keymapper.system.inputmethod

import android.view.InputDevice
import io.github.sds100.keymapper.util.InputEventType

/**
 * Created by sds100 on 21/04/2021.
 */
data class InputKeyModel(
    val keyCode: Int,
    val inputType: InputEventType = InputEventType.DOWN_UP,
    val metaState: Int = 0,
    val deviceId: Int = 0,
    val scanCode: Int = 0,
    val repeat: Int = 0,
    val source: Int = InputDevice.SOURCE_UNKNOWN,
)
