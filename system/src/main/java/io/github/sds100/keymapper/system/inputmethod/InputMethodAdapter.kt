package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InputMethodAdapter {
    val isUserInputRequiredToChangeIme: Flow<Boolean>

    fun showImePicker(fromForeground: Boolean): KMResult<*>
    suspend fun enableIme(imeId: String): KMResult<*>

    suspend fun chooseImeWithoutUserInput(imeId: String): KMResult<ImeInfo>

    fun getInfoById(imeId: String): KMResult<ImeInfo>
    fun getInfoByPackageName(packageName: String): KMResult<ImeInfo>

    /**
     * The last used input method is first.
     */
    val inputMethodHistory: StateFlow<List<ImeInfo>>
    val inputMethods: StateFlow<List<ImeInfo>>
    val chosenIme: StateFlow<ImeInfo?>
}
