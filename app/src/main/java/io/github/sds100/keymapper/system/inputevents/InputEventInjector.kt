package io.github.sds100.keymapper.system.inputevents

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel

interface InputEventInjector {
    suspend fun inputKeyEvent(model: InputKeyModel)

    fun createInjectedKeyEvent(
        eventTime: Long,
        action: Int,
        model: InputKeyModel,
    ): KeyEvent {
        val source = when {
            InputEventUtils.isDpadKeyCode(model.keyCode) -> InputDevice.SOURCE_DPAD
            KeyEvent.isGamepadButton(model.keyCode) -> InputDevice.SOURCE_GAMEPAD
            else -> InputDevice.SOURCE_KEYBOARD
        }

        return KeyEvent(
            eventTime,
            eventTime,
            action,
            model.keyCode,
            model.repeat,
            model.metaState,
            model.deviceId,
            model.scanCode,
            0,
            // See issue #1683. Some apps ignore key events which do not have a source.
            source,
        )
    }
}
