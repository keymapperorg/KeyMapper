package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.suspendThen
import io.github.sds100.keymapper.common.utils.then
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class KeyMapperImeHelper @Inject constructor(
    private val imeAdapter: InputMethodAdapter,
    private val packageName: String
) {
    companion object {
        const val KEY_MAPPER_GUI_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.latin"

        private const val KEY_MAPPER_LEANBACK_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.leanback"

        private const val KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.hackers"

        const val MIN_SUPPORTED_GUI_KEYBOARD_VERSION_CODE: Int = 20
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

    suspend fun enableCompatibleInputMethods() {
        keyMapperImePackageList.forEach { packageName ->
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

    fun isCompatibleImeChosen(): Boolean = imeAdapter.chosenIme.value?.packageName in keyMapperImePackageList

    fun isCompatibleImeEnabled(): Boolean = imeAdapter.inputMethods
        .map { containsCompatibleIme(it) }
        .firstBlocking()

    private fun containsCompatibleIme(imeList: List<ImeInfo>): Boolean = imeList
        .filter { it.isEnabled }
        .any { it.packageName in keyMapperImePackageList }

    private fun getLastUsedCompatibleImeId(): Result<String> {
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
                Error.NoCompatibleImeEnabled
            }
        }
    }

    private fun getLastUsedIncompatibleImeId(): Result<String> {
        for (ime in imeAdapter.inputMethodHistory.firstBlocking()) {
            if (ime.packageName !in keyMapperImePackageList) {
                return Success(ime.id)
            }
        }

        return Error.NoIncompatibleKeyboardsInstalled
    }
}
