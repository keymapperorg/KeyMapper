package io.github.sds100.keymapper.system.inputmethod

data class ImeInfo(
    val id: String,
    val packageName: String,
    val label: String,
    val isEnabled: Boolean,
    val isChosen: Boolean,
)
