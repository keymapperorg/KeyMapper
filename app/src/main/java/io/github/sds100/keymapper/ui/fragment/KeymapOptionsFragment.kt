package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.BehaviorOption
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentKeymapOptionsBinding
import io.github.sds100.keymapper.slider
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.str
import kotlinx.android.synthetic.main.fragment_keymap_options.*

/**
 * Created by sds100 on 19/03/2020.
 */
class KeymapOptionsFragment(private val mKeymapId: Long) : Fragment() {

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    private val mController = Controller()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentKeymapOptionsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            mViewModel.checkBoxModels.observe(viewLifecycleOwner, {
                mController.checkBoxModels = it
            })

            mViewModel.sliderModels.observe(viewLifecycleOwner, {
                mController.sliderModels = it
            })

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //assign in onViewCreated in case context is required when building the models.
        epoxyRecyclerViewFlags.adapter = mController.adapter
    }

    private inner class Controller : EpoxyController() {

        var checkBoxModels: List<CheckBoxListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        var sliderModels: List<SliderListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            checkBoxModels.forEach {
                checkbox {
                    id(it.id)
                    primaryText(str(it.label))
                    isSelected(it.isChecked)

                    onClick { view ->
                        mViewModel.setTriggerOption(it.id, (view as CheckBox).isChecked)
                    }
                }
            }

            sliderModels.forEach {
                slider {
                    id(it.id)
                    label(str(it.label))
                    model(it.sliderModel)

                    onSliderChangeListener { _, value, fromUser ->
                        if (!fromUser) return@onSliderChangeListener

                        //If the user has selected to use the default value
                        if (value < int(it.sliderModel.min)) {
                            mViewModel.setTriggerOption(it.id, BehaviorOption.DEFAULT)
                        } else {
                            mViewModel.setTriggerOption(it.id, value.toInt())
                        }
                    }
                }
            }
        }
    }
}