package io.github.sds100.keymapper.common.ui


data class MultiChoiceItem<out ID>(val id: ID, val label: String, val isChecked: Boolean = false)
