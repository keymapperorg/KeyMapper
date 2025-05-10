package io.github.sds100.keymapper.system.inputevents

import android.view.KeyEvent
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel

interface InputEventInjector {
    suspend fun inputKeyEvent(model: InputKeyModel)

    fun createInjectedKeyEvent(
        eventTime: Long,
        action: Int,
        model: InputKeyModel,
    ): KeyEvent {
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
            model.source,
        )
    }
}
