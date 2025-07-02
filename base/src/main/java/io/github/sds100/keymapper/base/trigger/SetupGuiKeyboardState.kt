package io.github.sds100.keymapper.base.trigger

data class SetupGuiKeyboardState(
    val isKeyboardInstalled: Boolean,
    val isKeyboardEnabled: Boolean,
    val isKeyboardChosen: Boolean,
) {
    companion object {
        val DEFAULT = SetupGuiKeyboardState(
            isKeyboardInstalled = false,
            isKeyboardEnabled = false,
            isKeyboardChosen = false,
        )
    }
}
