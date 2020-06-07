package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.TextBlockActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentEdittextBinding
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.setLiveDataEvent
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 30/03/2020.
 */

class TextBlockActionTypeFragment : Fragment() {
    companion object {
        const val SAVED_STATE_KEY = "key_text_block_saved_state"
    }

    private val mViewModel: TextBlockActionTypeViewModel by activityViewModels {
        InjectorUtils.provideTextBlockActionTypeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentEdittextBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner

            text = mViewModel.text
            caption = str(R.string.caption_action_type_text)

            setOnDoneClick {
                findNavController().apply {
                    currentBackStackEntry?.setLiveDataEvent(SAVED_STATE_KEY, mViewModel.text.value)
                }
            }

            return this.root
        }
    }
}