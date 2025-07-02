package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import javax.inject.Inject

class ShowInputMethodPickerUseCaseImpl @Inject constructor(
    private val inputMethodAdapter: InputMethodAdapter,
) : ShowInputMethodPickerUseCase {
    override fun show(fromForeground: Boolean) {
        inputMethodAdapter.showImePicker(fromForeground = fromForeground)
    }
}

interface ShowInputMethodPickerUseCase {
    fun show(fromForeground: Boolean)
}
