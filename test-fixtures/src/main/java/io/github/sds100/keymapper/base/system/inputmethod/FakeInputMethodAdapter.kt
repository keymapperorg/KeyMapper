package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.ImeSubtypeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInputMethodAdapter : InputMethodAdapter {

    override val inputMethodHistory = MutableStateFlow<List<ImeInfo>>(emptyList())

    override val inputMethods = MutableStateFlow<List<ImeInfo>>(emptyList())

    override val chosenIme = MutableStateFlow<ImeInfo?>(null)

    val enabledSubtypes = mutableMapOf<String, List<ImeSubtypeInfo>>()
    val lastUsedSubtypes = mutableMapOf<String, ImeSubtypeInfo>()
    var selectedSubtypeId: Int? = null

    override fun getChosenIme(): ImeInfo? {
        return chosenIme.value
    }

    override fun showImePicker(fromForeground: Boolean): KMResult<*> {
        return Success(Unit)
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

    override fun setDefaultInputMethod(imeId: String): KMResult<Unit> {
        chosenIme.value = inputMethods.value.firstOrNull { it.id == imeId }
        return Success(Unit)
    }

    override fun getEnabledSubtypes(imeId: String): KMResult<List<ImeSubtypeInfo>> {
        return Success(enabledSubtypes[imeId].orEmpty())
    }

    override fun getSelectedSubtype(): ImeSubtypeInfo? {
        return enabledSubtypes[chosenIme.value?.id].orEmpty()
            .firstOrNull { it.id == selectedSubtypeId }
    }

    override fun getLastUsedSubtype(imeId: String): ImeSubtypeInfo? {
        return lastUsedSubtypes[imeId]
    }

    override fun setSelectedSubtype(subtypeId: Int): KMResult<Unit> {
        selectedSubtypeId = subtypeId
        return Success(Unit)
    }

    override fun getNextInputMethod(): KMResult<ImeInfo> {
        val chosenImeId = chosenIme.value?.id
        val enabledImes = inputMethods.value.filter { it.isEnabled }

        if (enabledImes.isEmpty()) {
            return getInfoById(chosenImeId.orEmpty())
        }

        val currentIndex = enabledImes.indexOfFirst { it.id == chosenImeId }
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % enabledImes.size

        return Success(enabledImes[nextIndex])
    }
}
