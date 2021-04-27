package io.github.sds100.keymapper.system.url

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
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 30/03/2020.
 */

class ChooseUrlFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_url"
        const val EXTRA_URL = "extra_url"
    }

    private val viewModelChoose: ChooseUrlViewModel by activityViewModels {
        Inject.urlActionTypeViewModel()
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
            text = viewModelChoose.url
            caption = str(R.string.caption_action_type_url)

            editText.inputType = InputType.TYPE_TEXT_VARIATION_URI

            setOnDoneClick {
                setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_URL to viewModelChoose.url.value))
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}