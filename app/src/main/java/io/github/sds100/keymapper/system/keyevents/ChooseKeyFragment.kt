package io.github.sds100.keymapper.system.keyevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.databinding.FragmentKeyActionTypeBinding
import io.github.sds100.keymapper.util.Inject

/**
 * Created by sds100 on 30/03/2020.
 */

class ChooseKeyFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_key"
        const val EXTRA_KEYCODE = "extra_keycode"
    }

    private val viewModel: ChooseKeyViewModel by activityViewModels {
        Inject.keyActionTypeViewModel()
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentKeyActionTypeBinding? = null
    val binding: FragmentKeyActionTypeBinding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentKeyActionTypeBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        viewModel.clearKey()

        binding.setOnDoneClick {
            setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_KEYCODE to viewModel.keyCode.value))
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}