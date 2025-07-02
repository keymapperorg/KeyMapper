package io.github.sds100.keymapper.base.logging

import io.github.sds100.keymapper.base.utils.ui.TintType

data class LogEntryListItem(
    val id: Int,
    val time: String,
    val textTint: TintType,
    val message: String,
    val isSelected: Boolean,
)
