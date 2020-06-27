package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.navGraphViewModels
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentKeymapOptionsBinding
import io.github.sds100.keymapper.slider
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 19/03/2020.
 */
class KeymapOptionsFragment(private val mKeymapId: Long) : Fragment() {

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    private val mController = OptionsListController()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentKeymapOptionsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            epoxyRecyclerViewFlags.adapter = mController.adapter

            mViewModel.checkBoxOptions.observe(viewLifecycleOwner) {
                mController.keymapOptions = it
            }

            mViewModel.sliderOptions.observe(viewLifecycleOwner) { options ->
                val models = options.map {
                    val id = it.extraId
                    val value = it.value

                    SliderListItemModel(
                        id = id,
                        label = Extra.EXTRA_LABELS[id]!!,

                        sliderModel = SliderModel(
                            value = value,
                            isDefaultStepEnabled = true,
                            min = int(Extra.EXTRA_MIN_VALUES[id]!!),
                            max = int(Extra.EXTRA_MAX_VALUES[id]!!),
                            stepSize = int(Extra.EXTRA_STEP_SIZE_VALUES[id]!!))
                    )
                }

                mController.triggerOptions = models
            }

            return this.root
        }
    }

    private inner class OptionsListController : EpoxyController() {
        var triggerOptions: List<SliderListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        var keymapOptions: List<CheckBoxOption> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {

            keymapOptions.forEach {
                val flagId = it.flagId
                val isChecked = it.isChecked

                checkbox {
                    id(flagId)

                    val labelResId = KeyMap.KEYMAP_FLAG_LABEL_MAP[flagId]

                    if (labelResId != null) {
                        primaryText(str(labelResId))
                    }

                    isSelected(isChecked)

                    onClick { _ ->
                        mViewModel.toggleFlag(flagId)
                    }
                }
            }

            triggerOptions.forEach {
                slider {
                    id(it.id)
                    label(str(it.label))
                    model(it.sliderModel)

                    onSliderChangeListener { _, value, fromUser ->
                        if (!fromUser) return@onSliderChangeListener

                        //If the user has selected to use the default value
                        if (value < it.sliderModel.min) {
                            mViewModel.setTriggerExtraValue(it.id, ConfigKeymapViewModel.EXTRA_USE_DEFAULT)
                        } else {
                            mViewModel.setTriggerExtraValue(it.id, value.toInt())
                        }
                    }
                }
            }
        }
    }
}