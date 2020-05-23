package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.UrlActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentEdittextBinding
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.setLiveDataEvent
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 30/03/2020.
 */

class UrlActionTypeFragment : Fragment() {
    companion object {
        const val SAVED_STATE_KEY = "key_url_saved_state"
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
                findNavController().apply {
                    currentBackStackEntry?.setLiveDataEvent(SAVED_STATE_KEY, mViewModel.url.value)
                }
            }

            return this.root
        }
    }
}