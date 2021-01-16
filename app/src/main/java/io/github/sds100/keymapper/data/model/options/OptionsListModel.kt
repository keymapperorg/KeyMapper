package io.github.sds100.keymapper.data.model.options

import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel

/**
 * Created by sds100 on 15/01/21.
 */
data class OptionsListModel(val checkBoxModels: List<CheckBoxListItemModel>,
                            val sliderModels: List<SliderListItemModel>) {
    companion object {
        val EMPTY = OptionsListModel(emptyList(), emptyList())
    }
}