package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel

data class KeyMapListItemModel(
    val uid: String,
    val isSelected: Boolean,
    val triggerDescription: String?,
    val triggerErrors: List<TriggerError>,
    val actions: List<ComposeChipModel>,
    val constraints: List<ComposeChipModel>,
    val optionsDescription: String?,
    val extraInfo: String?,
)
