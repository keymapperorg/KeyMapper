package io.github.sds100.keymapper.ui.adapter

import android.content.Context
import android.widget.CheckBox
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.options.IntOption
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.slider
import io.github.sds100.keymapper.util.editTextNumberAlertDialog
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 29/11/20.
 */

abstract class OptionsController(lifecycleOwner: LifecycleOwner) : EpoxyController(), LifecycleOwner by lifecycleOwner {

    abstract val ctx: Context
    abstract val viewModel: BaseOptionsViewModel<*>

    var checkBoxModels: List<CheckBoxListItemModel> = emptyList()
        set(value) {
            field = value
            requestModelBuild()
        }

    var sliderModels: List<SliderListItemModel> = emptyList()
        set(value) {
            field = value
            requestModelBuild()
        }

    override fun buildModels() {
        checkBoxModels.forEach {
            checkbox {
                id(it.id)
                primaryText(ctx.str(it.label))
                isSelected(it.isChecked)

                onClick { view ->
                    viewModel.setValue(it.id, (view as CheckBox).isChecked)
                }
            }
        }

        sliderModels.forEach {
            slider {
                id(it.id)
                label(ctx.str(it.label))
                model(it.sliderModel)

                onSliderChangeListener { _, value, fromUser ->
                    if (!fromUser) return@onSliderChangeListener

                    //If the user has selected to use the default value
                    if (value < ctx.int(it.sliderModel.min)) {
                        viewModel.setValue(it.id, IntOption.DEFAULT)
                    } else {
                        viewModel.setValue(it.id, value.toInt())
                    }
                }

                onSliderValueClickListener { _ ->
                    lifecycleScope.launchWhenStarted {
                        val num = ctx.editTextNumberAlertDialog(
                            this@OptionsController,
                            hint = ctx.str(it.label),
                            min = ctx.int(it.sliderModel.min))

                        viewModel.setValue(it.id, num)
                    }
                }
            }
        }
    }
}