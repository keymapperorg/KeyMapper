package io.github.sds100.keymapper.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyControllerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsDialogViewModel
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.ui.adapter.OptionsController

/**
 * Created by sds100 on 27/06/2020.
 */

abstract class BaseOptionsDialogFragment<BINDING : ViewDataBinding, O : BaseOptions<*>>
    : BottomSheetDialogFragment() {

    companion object {
        const val EXTRA_OPTIONS = "extra_options"
    }

    abstract val optionsViewModel: BaseOptionsDialogViewModel<O>
    abstract val requestKey: String
    abstract val initialOptions: O

    private val controller by lazy {
        object : OptionsController(this) {
            override val activity: FragmentActivity
                get() = requireActivity()

            override val ctx: Context
                get() = requireContext()

            override val viewModel: BaseOptionsViewModel<*>
                get() = optionsViewModel
        }
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: BINDING? = null
    val binding: BINDING
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        optionsViewModel.setOptions(initialOptions)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bind(inflater, container).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.apply {
            optionsViewModel.checkBoxModels.observe(viewLifecycleOwner, {
                controller.checkBoxModels = it
            })

            optionsViewModel.sliderModels.observe(viewLifecycleOwner, {
                controller.sliderModels = it
            })

            optionsViewModel.onSaveEvent.observe(viewLifecycleOwner, {
                setFragmentResult(requestKey, bundleOf(EXTRA_OPTIONS to it))
                findNavController().navigateUp()
            })

            setRecyclerViewAdapter(this, controller.adapter)
            subscribeCustomUi(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        optionsViewModel.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState ?: return

        optionsViewModel.restoreState(savedInstanceState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    abstract fun subscribeCustomUi(binding: BINDING)
    abstract fun setRecyclerViewAdapter(binding: BINDING, adapter: EpoxyControllerAdapter)
    abstract fun bind(inflater: LayoutInflater, container: ViewGroup?): BINDING
}