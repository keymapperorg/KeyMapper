package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.data.viewmodel.KeyActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentKeyActionTypeBinding
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.setLiveData

/**
 * Created by sds100 on 30/03/2020.
 */

class KeyActionTypeFragment : Fragment() {
    companion object {
        const val SAVED_STATE_KEY = "key_key_saved_state"
    }

    private val mViewModel: KeyActionTypeViewModel by activityViewModels {
        InjectorUtils.provideKeyActionTypeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentKeyActionTypeBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            mViewModel.clearKey()

            keyEventLabel = mViewModel.keyLabel

            setOnDoneClick {
                findNavController().apply {
                    currentBackStackEntry?.setLiveData(SAVED_STATE_KEY, mViewModel.keyEvent.value?.keyCode)
                }
            }

            return this.root
        }
    }
}