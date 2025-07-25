package io.github.sds100.keymapper.system.inputevents

import android.view.KeyEvent
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel

interface InputEventInjector {
    suspend fun inputKeyEvent(model: InputKeyModel)
}


/**
 * Create a KeyEvent instance that can be injected into the Android system.
 */
fun InputEventInjector.createKeyEvent(
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