package io.github.sds100.keymapper.base.utils.ui


data class RadioButtonPairListItem(
    override val id: String,
    val header: String,

    val leftButtonId: String,
    val leftButtonText: String,
    val leftButtonChecked: Boolean,

    val rightButtonId: String,
    val rightButtonText: String,
    val rightButtonChecked: Boolean,
) : ListItem
