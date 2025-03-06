package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel

data class KeyMapListItemModel(
    val isSelected: Boolean,
    val content: Content,
) {
    val uid = content.uid

    data class Content(
        val uid: String,
        val triggerDescription: String?,
        val triggerErrors: List<TriggerError>,
        val actions: List<ComposeChipModel>,
        val constraintMode: ConstraintMode,
        val constraints: List<ComposeChipModel>,
        val optionsDescription: String?,
        val extraInfo: String?,
    )
}
