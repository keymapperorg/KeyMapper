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

    fun setDefaultInputMethod(imeId: String): KMResult<Unit>

    fun getEnabledSubtypes(imeId: String): KMResult<List<ImeSubtypeInfo>>

    /**
     * @return The selected subtype of the chosen input method.
     */
    fun getSelectedSubtype(): ImeSubtypeInfo?

    /**
     * @return The subtype the input method had when it was last chosen.
     */
    fun getLastUsedSubtype(imeId: String): ImeSubtypeInfo?

    fun setSelectedSubtype(subtypeId: Int): KMResult<Unit>

    /**
     * Returns the input method that should become active next when cycling through all
     * currently enabled input methods, in a fixed order, wrapping around after the last one.
     */
    fun getNextInputMethod(): KMResult<ImeInfo>
}
