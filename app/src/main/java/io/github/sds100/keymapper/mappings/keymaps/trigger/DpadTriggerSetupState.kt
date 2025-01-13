package io.github.sds100.keymapper.mappings.keymaps.trigger

data class DpadTriggerSetupState(
    val isKeyboardInstalled: Boolean,
    val isKeyboardEnabled: Boolean,
    val isKeyboardChosen: Boolean,
) {
    companion object {
        val DEFAULT = DpadTriggerSetupState(
            isKeyboardInstalled = false,
            isKeyboardEnabled = false,
            isKeyboardChosen = false,
        )
    }
}
