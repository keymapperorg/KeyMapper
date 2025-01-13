package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class SetupDpadTriggerUseCaseImpl(
    private val inputMethodAdapter: InputMethodAdapter,
    private val packageManager: PackageManagerAdapter,
) : SetupDpadTriggerUseCase {
    override val isGuiKeyboardInstalled: Flow<Boolean> =
        packageManager.installedPackages
            .mapNotNull { it as? State.Data }
            .map { packages -> packages.data.any { it.packageName == KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE } }

    override val isGuiKeyboardEnabled: Flow<Boolean> =
        getGuiKeyboardImeInfoFlow().map { it?.isEnabled ?: false }

    override fun enableGuiKeyboard() {
        inputMethodAdapter.getInfoByPackageName(KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE)
            .onSuccess {
                inputMethodAdapter.enableIme(it.id)
            }
    }

    override val isGuiKeyboardChosen: Flow<Boolean> =
        getGuiKeyboardImeInfoFlow().map { it?.isChosen ?: false }

    override fun chooseGuiKeyboard() {
        inputMethodAdapter.showImePicker(fromForeground = true)
    }

    private fun getGuiKeyboardImeInfoFlow(): Flow<ImeInfo?> {
        return inputMethodAdapter.inputMethods.map { list -> list.find { it.packageName == KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE } }
    }
}

interface SetupDpadTriggerUseCase {
    val isGuiKeyboardInstalled: Flow<Boolean>

    val isGuiKeyboardEnabled: Flow<Boolean>
    fun enableGuiKeyboard()

    val isGuiKeyboardChosen: Flow<Boolean>
    fun chooseGuiKeyboard()
}
