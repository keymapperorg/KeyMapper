package io.github.sds100.keymapper.system.inputmethod



class ShowInputMethodPickerUseCaseImpl(
    private val inputMethodAdapter: InputMethodAdapter,
) : ShowInputMethodPickerUseCase {
    override fun show(fromForeground: Boolean) {
        inputMethodAdapter.showImePicker(fromForeground = fromForeground)
    }
}

interface ShowInputMethodPickerUseCase {
    fun show(fromForeground: Boolean)
}
