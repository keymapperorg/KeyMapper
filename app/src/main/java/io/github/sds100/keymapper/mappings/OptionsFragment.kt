package io.github.sds100.keymapper.mappings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.databinding.FragmentOptionsBinding
import io.github.sds100.keymapper.divider
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.ui.utils.configuredCheckBox
import io.github.sds100.keymapper.ui.utils.configuredRadioButtonPair
import io.github.sds100.keymapper.ui.utils.configuredRadioButtonTriple
import io.github.sds100.keymapper.ui.utils.configuredSlider
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 12/04/2021.
 */
abstract class OptionsBottomSheetFragment : BottomSheetDialogFragment() {

    abstract val url: String
    abstract val viewModel: OptionsViewModel

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentOptionsBinding? = null
    private val binding: FragmentOptionsBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentOptionsBinding.inflate(inflater).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.state.collectLatest { state ->
                binding.showProgressBar = state.showProgressBar
                populateList(state.listItems)
            }
        }

        binding.setOnHelpClick {
            UrlUtils.openUrl(requireContext(), url)
        }

        binding.setOnDoneClick {
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun populateList(listItems: List<ListItem>) {
        binding.epoxyRecyclerView.withModels {
            listItems.forEach { model ->
                if (model is RadioButtonPairListItem) {
                    configuredRadioButtonPair(model) { id, isChecked ->
                        viewModel.setRadioButtonValue(id, isChecked)
                    }
                }

                if (model is RadioButtonTripleListItem) {
                    configuredRadioButtonTriple(model) { id, isChecked ->
                        viewModel.setRadioButtonValue(id, isChecked)
                    }
                }

                if (model is CheckBoxListItem) {
                    configuredCheckBox( model) {
                        viewModel.setCheckboxValue(model.id, it)
                    }
                }

                if (model is SliderListItem) {
                    configuredSlider(this@OptionsBottomSheetFragment, model) {
                        viewModel.setSliderValue(model.id, it)
                    }
                }

                if (model is DividerListItem) {
                    divider { id(model.id) }
                }
            }
        }
    }
}