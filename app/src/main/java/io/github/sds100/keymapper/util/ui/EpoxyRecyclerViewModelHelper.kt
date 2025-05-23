package io.github.sds100.keymapper.util.ui

import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.databinding.ListItemCheckboxBinding

/**
 * Created by sds100 on 20/03/2021.
 */

fun EpoxyController.configuredCheckBox(
    model: CheckBoxListItem,
    onCheckedChange: (checked: Boolean) -> Unit,
) {
    checkbox {
        id(model.id)
        model(model)

        onBind { bindingModel, view, _ ->

            (view.dataBinding as ListItemCheckboxBinding).checkBox.apply {
                // this is very important so checkboxes in other recycler views aren't affected by the checked state changing.
                setOnCheckedChangeListener(null)

                isChecked = bindingModel.model().isChecked
                text = bindingModel.model().label

                setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange.invoke(isChecked)
                }
            }
        }
    }
}
