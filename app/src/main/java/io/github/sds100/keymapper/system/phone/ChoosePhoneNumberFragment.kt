package io.github.sds100.keymapper.system.phone

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentEdittextBinding
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 30/03/2020.
 */

class ChoosePhoneNumberFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_phone_call_number"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }

    private val viewModel: ChoosePhoneNumberViewModel by activityViewModels {
        ChoosePhoneNumberViewModel.Factory()
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentEdittextBinding? = null
    val binding: FragmentEdittextBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentEdittextBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            text = viewModel.number
            caption = str(R.string.caption_action_type_phone_call)
            editText.inputType = InputType.TYPE_CLASS_PHONE

            setOnDoneClick {
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(EXTRA_PHONE_NUMBER to viewModel.number.value))

                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}