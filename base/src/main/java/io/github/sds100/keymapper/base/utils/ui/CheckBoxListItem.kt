package io.github.sds100.keymapper.base.utils.ui

data class CheckBoxListItem(
    override val id: String,
    val isChecked: Boolean,
    val label: String,
) : ListItem
