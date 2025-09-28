package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.StateFlow

interface InputMethodAdapter {
    fun showImePicker(fromForeground: Boolean): KMResult<*>

    fun getInfoById(imeId: String): KMResult<ImeInfo>
    fun getInfoByPackageName(packageName: String): KMResult<ImeInfo>

    /**
     * The last used input method is first.
     */
    val inputMethodHistory: StateFlow<List<ImeInfo>>
    val inputMethods: StateFlow<List<ImeInfo>>
    val chosenIme: StateFlow<ImeInfo?>

    fun getChosenIme(): ImeInfo?
}
