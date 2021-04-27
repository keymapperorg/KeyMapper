package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.ui.ListItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 12/04/2021.
 */

interface OptionsViewModel {
    val state: StateFlow<OptionsUiState>

    fun setRadioButtonValue(id: String, value: Boolean)
    fun setSliderValue(id: String, value: Defaultable<Int>)
    fun setCheckboxValue(id: String, value: Boolean)
}

data class OptionsUiState(
    val showProgressBar: Boolean = false,
    val listItems: List<ListItem> = emptyList(),
)