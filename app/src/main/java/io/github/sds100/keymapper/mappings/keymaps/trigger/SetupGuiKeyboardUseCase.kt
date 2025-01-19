package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.system.apps.PackageInfo
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
    private val guiKeyboardPackage: Flow<PackageInfo?> =
        packageManager.installedPackages
            .mapNotNull { it as? State.Data }
            .map { packages -> packages.data.find { it.packageName == KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE } }

    override val isInstalled: Flow<Boolean> = guiKeyboardPackage.map { it != null }

    override val isEnabled: Flow<Boolean> =
        getGuiKeyboardImeInfoFlow().map { it?.isEnabled ?: false }

    override val isCompatibleVersion: Flow<Boolean> =
        guiKeyboardPackage.map { packageInfo ->
            if (packageInfo == null) {
                false
            } else {
                packageInfo.versionCode >= KeyMapperImeHelper.MIN_SUPPORTED_GUI_KEYBOARD_VERSION_CODE
            }
        }

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

    val isCompatibleVersion: Flow<Boolean>
}
