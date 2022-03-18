package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 26/07/2021.
 */
data class MultiChoiceItem<out ID>(val id: ID, val label: String, val isChecked: Boolean = false)
