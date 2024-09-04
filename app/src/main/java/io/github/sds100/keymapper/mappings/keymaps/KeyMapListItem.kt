package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.util.ui.ChipUi

/**
 * Created by sds100 on 28/03/2020.
 */

data class KeyMapListItem(
    val keyMapUiState: KeyMapUiState,
    val selectionUiState: SelectionUiState,
) {
    data class KeyMapUiState(
        val uid: String,
        val actionChipList: List<ChipUi>,
        val constraintChipList: List<ChipUi>,
        val triggerDescription: String,
        val optionsDescription: String,
        val extraInfo: String,
        val triggerErrorChipList: List<ChipUi>,
    ) {
        val hasTrigger: Boolean
            get() = triggerDescription.isNotBlank()

        val hasOptions: Boolean
            get() = optionsDescription.isNotBlank()
    }

    data class SelectionUiState(val isSelected: Boolean, val isSelectable: Boolean)
}
