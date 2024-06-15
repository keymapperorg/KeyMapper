package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 12/04/2021.
 */
data class RadioButtonTripleListItem(
    override val id: String,
    val header: String,

    val leftButtonId: String,
    val leftButtonText: String,
    val leftButtonChecked: Boolean,

    val centerButtonId: String,
    val centerButtonText: String,
    val centerButtonChecked: Boolean,

    val rightButtonId: String,
    val rightButtonText: String,
    val rightButtonChecked: Boolean,
) : ListItem
