package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.util.ui.ChipUi

/**
 * Created by sds100 on 08/11/20.
 */

data class FingerprintMapListItem(
    val id: FingerprintGestureType,
    val header: String,
    val actionChipList: List<ChipUi>,
    val constraintChipList: List<ChipUi>,
    val optionsDescription: String,
    val isEnabled: Boolean,
    val extraInfo: String,
) {
    val hasOptions: Boolean
        get() = optionsDescription.isNotBlank()
}
