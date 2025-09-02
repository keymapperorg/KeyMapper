package io.github.sds100.keymapper.base.utils.ui

data class MultiChoiceItem<out ID>(val id: ID, val label: String, val isChecked: Boolean = false)
