package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyControllerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.model.options.BehaviorOption
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.slider
import io.github.sds100.keymapper.util.collectWhenLifecycleStarted
import io.github.sds100.keymapper.util.editTextNumberAlertDialog
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 27/06/2020.
 */
abstract class BaseOptionsFragment<BINDING : ViewDataBinding, O : BaseOptions<*>> : BottomSheetDialogFragment() {

    companion object {
        const val EXTRA_OPTIONS = "extra_options"
    }

    abstract val viewModel: BaseOptionsViewModel<O>
    abstract val requestKey: String
    abstract val initialOptions: O

    private val mController by lazy { Controller() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setOptions(initialOptions)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        bind(inflater, container).apply {
            subscribeUi(this)

            return root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState ?: return

        viewModel.restoreState(savedInstanceState)
    }

    open fun subscribeUi(binding: BINDING) = binding.apply {
        lifecycleOwner = viewLifecycleOwner

        setRecyclerViewAdapter(binding, mController.adapter)

        viewModel.checkBoxModels.observe(viewLifecycleOwner, {
            mController.checkBoxModels = it
        })

        viewModel.sliderModels.observe(viewLifecycleOwner, {
            mController.sliderModels = it
        })

        viewModel.onSaveEvent.collectWhenLifecycleStarted(viewLifecycleOwner) {
            setFragmentResult(requestKey, bundleOf(EXTRA_OPTIONS to it))
            findNavController().navigateUp()
        }
    }

    abstract fun setRecyclerViewAdapter(binding: BINDING, adapter: EpoxyControllerAdapter)
    abstract fun bind(inflater: LayoutInflater, container: ViewGroup?): BINDING

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
                        viewModel.setValue(it.id, (view as CheckBox).isChecked)
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
                            viewModel.setValue(it.id, BehaviorOption.DEFAULT)
                        } else {
                            viewModel.setValue(it.id, value.toInt())
                        }
                    }

                    onSliderValueClickListener { _ ->
                        lifecycleScope.launchWhenStarted {
                            val num = requireActivity().editTextNumberAlertDialog(
                                hint = str(it.label),
                                min = int(it.sliderModel.min))

                            viewModel.setValue(it.id, num)
                        }
                    }
                }
            }
        }
    }
}