package io.github.sds100.keymapper.system.inputmethod

/**
 * Created by sds100 on 23/03/2021.
 */
data class ImeInfo(
    val id: String,
    val packageName: String,
    val label: String,
    val isEnabled: Boolean,
    val isChosen: Boolean,
)
