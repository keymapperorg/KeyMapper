package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KeyMapperImeHelper(
    private val switchImeInterface: SwitchImeInterface,
    private val imeAdapter: InputMethodAdapter,
    private val packageName: String,
) {
    companion object {
        const val KEY_MAPPER_GUI_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.latin"

        private const val KEY_MAPPER_LEANBACK_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.leanback"

        private const val KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.hackers"

        const val MIN_SUPPORTED_GUI_KEYBOARD_VERSION_CODE: Int = 20

        fun isKeyMapperInputMethod(imePackage: String, keyMapperPackageName: String): Boolean {
            return imePackage == keyMapperPackageName ||
                imePackage == KEY_MAPPER_GUI_IME_PACKAGE ||
                imePackage == KEY_MAPPER_LEANBACK_IME_PACKAGE ||
                imePackage == KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE
        }
    }

    private val keyMapperImePackageList = arrayOf(
        packageName,
        KEY_MAPPER_GUI_IME_PACKAGE,
        KEY_MAPPER_LEANBACK_IME_PACKAGE,
        KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE,
    )

    val isCompatibleImeEnabledFlow: Flow<Boolean> =
        imeAdapter.inputMethods
            .map { containsCompatibleIme(it) }

    val isCompatibleImeChosenFlow: Flow<Boolean> =
        imeAdapter.chosenIme
            .map { chosenIme ->
                if (chosenIme == null) {
                    false
                } else {
                    isKeyMapperInputMethod(chosenIme.packageName, packageName)
                }
            }

    fun enableCompatibleInputMethod(): KMResult<Unit> {
        var result: KMResult<Unit>? = null

        for (imePackageName in keyMapperImePackageList) {
            val imeId =
                imeAdapter.getInfoByPackageName(imePackageName).valueOrNull()?.id ?: continue

            result = switchImeInterface.enableIme(imeId)

            // Stop trying to enable IMEs if one is enabled.
            if (result.isSuccess) {
                break
            }
        }

        return result ?: KMError.InputMethodNotFound(packageName)
    }

    fun chooseCompatibleInputMethod(): KMResult<String> =
        getLastUsedCompatibleImeId().then { imeId ->
            switchImeInterface.switchIme(imeId).then { Success(imeId) }
        }

    fun chooseLastUsedIncompatibleInputMethod(): KMResult<String> =
        getLastUsedIncompatibleImeId().then { imeId ->
            switchImeInterface.switchIme(imeId).then { Success(imeId) }
        }

    fun toggleCompatibleInputMethod(): KMResult<String> {
        return if (isCompatibleImeChosen()) {
            chooseLastUsedIncompatibleInputMethod()
        } else {
            chooseCompatibleInputMethod()
        }
    }

    fun isCompatibleImeChosen(): Boolean {
        val chosenIme = imeAdapter.getChosenIme() ?: return false

        return isKeyMapperInputMethod(chosenIme.packageName, packageName)
    }

    fun isCompatibleImeEnabled(): Boolean = imeAdapter.inputMethods
        .map { containsCompatibleIme(it) }
        .firstBlocking()

    private fun containsCompatibleIme(imeList: List<ImeInfo>): Boolean = imeList
        .filter { it.isEnabled }
        .any { it.packageName in keyMapperImePackageList }

    private fun getLastUsedCompatibleImeId(): KMResult<String> {
        for (ime in imeAdapter.inputMethodHistory.firstBlocking()) {
            if (ime.packageName in keyMapperImePackageList && ime.isEnabled) {
                return Success(ime.id)
            }
        }

        imeAdapter.getInfoByPackageName(KEY_MAPPER_GUI_IME_PACKAGE).onSuccess { ime ->
            if (ime.isEnabled) {
                return Success(ime.id)
            }
        }

        return imeAdapter.getInfoByPackageName(packageName).then { ime ->
            if (ime.isEnabled) {
                Success(ime.id)
            } else {
                KMError.NoCompatibleImeEnabled
            }
        }
    }

    private fun getLastUsedIncompatibleImeId(): KMResult<String> {
        for (ime in imeAdapter.inputMethodHistory.firstBlocking()) {
            if (ime.packageName !in keyMapperImePackageList) {
                return Success(ime.id)
            }
        }

        return KMError.NoIncompatibleKeyboardsInstalled
    }
}
