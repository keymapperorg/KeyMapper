package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 14/02/2021.
 */
interface InputMethodAdapter {
    val isUserInputRequiredToChangeIme: Flow<Boolean>

    fun showImePicker(fromForeground: Boolean): Result<*>
    fun enableIme(imeId: String): Result<*>

    suspend fun chooseImeWithoutUserInput(imeId: String): Result<ImeInfo>

    fun getInfoById(imeId: String): Result<ImeInfo>
    fun getInfoByPackageName(packageName: String): Result<ImeInfo>

    /**
     * The last used input method is first.
     */
    val inputMethodHistory: StateFlow<List<ImeInfo>>
    val inputMethods: StateFlow<List<ImeInfo>>
    val chosenIme: StateFlow<ImeInfo?>
}
