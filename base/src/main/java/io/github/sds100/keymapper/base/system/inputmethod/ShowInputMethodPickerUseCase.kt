package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShowInputMethodPickerUseCaseImpl @Inject constructor(
    private val inputMethodAdapter: InputMethodAdapter,
    private val preferenceRepository: PreferenceRepository,
) : ShowInputMethodPickerUseCase {
    override val isAutoSwitchImeEnabled: Flow<Boolean> =
        preferenceRepository.get(Keys.changeImeOnInputFocus)
            .map { it ?: PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS }

    override fun disableAutoSwitch() {
        preferenceRepository.set(Keys.changeImeOnInputFocus, false)
    }

    override fun show(fromForeground: Boolean) {
        inputMethodAdapter.showImePicker(fromForeground = fromForeground)
    }
}

interface ShowInputMethodPickerUseCase {
    val isAutoSwitchImeEnabled: Flow<Boolean>

    fun disableAutoSwitch()
    fun show(fromForeground: Boolean)
}
