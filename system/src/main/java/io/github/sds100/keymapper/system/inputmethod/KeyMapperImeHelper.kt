package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.suspendThen
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.base.utils.firstBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KeyMapperImeHelper(private val imeAdapter: InputMethodAdapter) {
    companion object {
        const val KEY_MAPPER_GUI_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.latin"

        private const val KEY_MAPPER_LEANBACK_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.leanback"

        private const val KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.hackers"

        val KEY_MAPPER_IME_PACKAGE_LIST = arrayOf(
            Constants.PACKAGE_NAME,
            KEY_MAPPER_GUI_IME_PACKAGE,
            KEY_MAPPER_LEANBACK_IME_PACKAGE,
            KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE,
        )

        const val MIN_SUPPORTED_GUI_KEYBOARD_VERSION_CODE: Int = 20
    }

    val isCompatibleImeEnabledFlow: Flow<Boolean> =
        imeAdapter.inputMethods
            .map { containsCompatibleIme(it) }

    suspend fun enableCompatibleInputMethods() {
        KEY_MAPPER_IME_PACKAGE_LIST.forEach { packageName ->
            imeAdapter.getInfoByPackageName(packageName).onSuccess {
                imeAdapter.enableIme(it.id)
            }
        }
    }

    suspend fun chooseCompatibleInputMethod(): Result<ImeInfo> = getLastUsedCompatibleImeId().suspendThen {
        imeAdapter.chooseImeWithoutUserInput(it)
    }

    suspend fun chooseLastUsedIncompatibleInputMethod(): Result<ImeInfo> = getLastUsedIncompatibleImeId().then {
        imeAdapter.chooseImeWithoutUserInput(it)
    }

    suspend fun toggleCompatibleInputMethod(): Result<ImeInfo> = if (isCompatibleImeChosen()) {
        chooseLastUsedIncompatibleInputMethod()
    } else {
        chooseCompatibleInputMethod()
    }

    fun isCompatibleImeChosen(): Boolean = imeAdapter.chosenIme.value?.packageName in KEY_MAPPER_IME_PACKAGE_LIST

    fun isCompatibleImeEnabled(): Boolean = imeAdapter.inputMethods
        .map { containsCompatibleIme(it) }
        .firstBlocking()

    private fun containsCompatibleIme(imeList: List<ImeInfo>): Boolean = imeList
        .filter { it.isEnabled }
        .any { it.packageName in KEY_MAPPER_IME_PACKAGE_LIST }

    private fun getLastUsedCompatibleImeId(): Result<String> {
        for (ime in imeAdapter.inputMethodHistory.firstBlocking()) {
            if (ime.packageName in KEY_MAPPER_IME_PACKAGE_LIST && ime.isEnabled) {
                return Success(ime.id)
            }
        }

        imeAdapter.getInfoByPackageName(KEY_MAPPER_GUI_IME_PACKAGE).onSuccess { ime ->
            if (ime.isEnabled) {
                return Success(ime.id)
            }
        }

        return imeAdapter.getInfoByPackageName(Constants.PACKAGE_NAME).then { ime ->
            if (ime.isEnabled) {
                Success(ime.id)
            } else {
                Error.NoCompatibleImeEnabled
            }
        }
    }

    private fun getLastUsedIncompatibleImeId(): Result<String> {
        for (ime in imeAdapter.inputMethodHistory.firstBlocking()) {
            if (ime.packageName !in KEY_MAPPER_IME_PACKAGE_LIST) {
                return Success(ime.id)
            }
        }

        return Error.NoIncompatibleKeyboardsInstalled
    }
}
