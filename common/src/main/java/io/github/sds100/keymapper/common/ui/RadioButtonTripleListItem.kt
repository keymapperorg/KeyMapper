package io.github.sds100.keymapper.common.ui


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
