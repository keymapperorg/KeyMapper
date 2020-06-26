package io.github.sds100.keymapper.ui.fragment

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
import io.github.sds100.keymapper.data.viewmodel.UrlActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentEdittextBinding
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 30/03/2020.
 */

class UrlActionTypeFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_url"
        const val EXTRA_URL = "extra_url"
    }

    private val mViewModel: UrlActionTypeViewModel by activityViewModels {
        InjectorUtils.provideUrlActionTypeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentEdittextBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner

            text = mViewModel.url
            caption = str(R.string.caption_action_type_url)

            editText.inputType = InputType.TYPE_TEXT_VARIATION_URI

            setOnDoneClick {
                setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_URL to mViewModel.url.value))
                findNavController().navigateUp()
            }

            return this.root
        }
    }
}