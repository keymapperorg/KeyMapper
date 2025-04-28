package io.github.sds100.keymapper.system.inputevents

import io.github.sds100.keymapper.system.inputmethod.InputKeyModel

interface InputEventInjector {
    suspend fun inputKeyEvent(model: InputKeyModel)
}
