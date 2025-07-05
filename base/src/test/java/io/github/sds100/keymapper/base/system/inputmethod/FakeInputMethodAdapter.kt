package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInputMethodAdapter : InputMethodAdapter {

    override val inputMethodHistory = MutableStateFlow<List<ImeInfo>>(emptyList())

    override val inputMethods = MutableStateFlow<List<ImeInfo>>(emptyList())

    override val chosenIme = MutableStateFlow<ImeInfo?>(null)

    override val isUserInputRequiredToChangeIme = MutableStateFlow(false)

    override fun showImePicker(fromForeground: Boolean): KMResult<*> {
        return Success(Unit)
    }

    override suspend fun enableIme(imeId: String): KMResult<*> {
        return Success(Unit)
    }

    override suspend fun chooseImeWithoutUserInput(imeId: String): KMResult<ImeInfo> {
        return inputMethods.value
            .firstOrNull { it.id == imeId }
            ?.let { Success(it) }
            ?: KMError.InputMethodNotFound(imeId)
    }

    override fun getInfoById(imeId: String): KMResult<ImeInfo> {
        return inputMethods.value
            .firstOrNull { it.id == imeId }
            ?.let { Success(it) }
            ?: KMError.InputMethodNotFound(imeId)
    }

    override fun getInfoByPackageName(packageName: String): KMResult<ImeInfo> {
        return inputMethods.value
            .firstOrNull { it.packageName == packageName }
            ?.let { Success(it) }
            ?: KMError.InputMethodNotFound(packageName)
    }
}