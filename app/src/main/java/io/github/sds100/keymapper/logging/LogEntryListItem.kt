package io.github.sds100.keymapper.logging

import io.github.sds100.keymapper.util.ui.TintType

/**
 * Created by sds100 on 14/05/2021.
 */
data class LogEntryListItem(
    val id: Int,
    val time: String,
    val textTint: TintType,
    val message: String,
    val isSelected: Boolean,
)
