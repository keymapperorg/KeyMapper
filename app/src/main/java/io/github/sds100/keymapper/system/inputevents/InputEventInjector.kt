package io.github.sds100.keymapper.system.inputevents

import io.github.sds100.keymapper.system.inputmethod.InputKeyModel

/**
 * Created by sds100 on 21/04/2021.
 */

interface InputEventInjector {
    fun inputKeyEvent(model: InputKeyModel)
}
