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

class SetupGuiKeyboardUseCaseImpl(
    private val inputMethodAdapter: InputMethodAdapter,
    private val packageManager: PackageManagerAdapter,
) : SetupGuiKeyboardUseCase {
    override val isInstalled: Flow<Boolean> =
        packageManager.installedPackages
            .mapNotNull { it as? State.Data }
            .map { packages -> packages.data.any { it.packageName == KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE } }

    override val isEnabled: Flow<Boolean> =
        getGuiKeyboardImeInfoFlow().map { it?.isEnabled ?: false }

    override fun enableInputMethod() {
        inputMethodAdapter.getInfoByPackageName(KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE)
            .onSuccess {
                inputMethodAdapter.enableIme(it.id)
            }
    }

    override val isChosen: Flow<Boolean> =
        getGuiKeyboardImeInfoFlow().map { it?.isChosen ?: false }

    override fun chooseInputMethod() {
        inputMethodAdapter.showImePicker(fromForeground = true)
    }

    private fun getGuiKeyboardImeInfoFlow(): Flow<ImeInfo?> {
        return inputMethodAdapter.inputMethods.map { list -> list.find { it.packageName == KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE } }
    }
}

interface SetupGuiKeyboardUseCase {
    val isInstalled: Flow<Boolean>

    val isEnabled: Flow<Boolean>
    fun enableInputMethod()

    val isChosen: Flow<Boolean>
    fun chooseInputMethod()
}
