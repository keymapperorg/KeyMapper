package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
import io.github.sds100.keymapper.databinding.FragmentMenuBinding
import io.github.sds100.keymapper.util.FeedbackUtils
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.KeyboardUtils

class MenuFragment : BottomSheetDialogFragment() {

    private val mViewModel: KeymapListViewModel by viewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentMenuBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner

            setChangeKeyboard {
                KeyboardUtils.showInputMethodPicker()
                dismiss()
            }

            setSendFeedback {
                FeedbackUtils.sendFeedback()
                dismiss()
            }

            setOpenSettings {
                findNavController().navigate(R.id.action_global_settingsFragment)
                dismiss()
            }

            setOpenAbout {
                dismiss()
                findNavController().navigate(R.id.action_global_aboutFragment)
            }

            setEnableAll {
                mViewModel.enableAll()
                dismiss()
            }

            setDisableAll {
                mViewModel.disableAll()
                dismiss()
            }

            return this.root
        }
    }
}