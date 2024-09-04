package io.github.sds100.keymapper.util.ui

data class SliderListItem(
    override val id: String,
    val label: String,
    val sliderModel: SliderModel,
) : ListItem
