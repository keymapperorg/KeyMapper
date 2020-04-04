package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.HelpViewModel
import io.github.sds100.keymapper.databinding.FragmentHelpBinding
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.result.onFailure
import io.github.sds100.keymapper.util.result.onSuccess
import splitties.toast.toast

class HelpFragment : BottomSheetDialogFragment() {

    private val mViewModel by activityViewModels<HelpViewModel> {
        InjectorUtils.provideHelpViewModel(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentHelpBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            progressCallback = mViewModel

            mViewModel.refreshIfFailed()

            mViewModel.markdownText.observe(viewLifecycleOwner) { result ->
                result.onSuccess {
                    markdown = it
                }.onFailure {
                    findNavController().navigateUp()
                    toast(R.string.error_download_failed)
                }
            }

            return this.root
        }
    }
}