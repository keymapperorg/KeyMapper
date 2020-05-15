package io.github.sds100.keymapper.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.data.viewmodel.OnlineFileViewModel
import io.github.sds100.keymapper.databinding.FragmentOnlineFileBinding
import io.github.sds100.keymapper.util.EventObserver
import io.github.sds100.keymapper.util.InjectorUtils
import splitties.resources.appStr
import splitties.toast.toast

class OnlineFileFragment : BottomSheetDialogFragment() {

    private val mArgs by navArgs<OnlineFileFragmentArgs>()

    private val mFileUrl by lazy { appStr(mArgs.StringNavArgFileUrl) }
    private val mAlternateUrl by lazy {
        if (mArgs.StringNavArgFileUrlAlt != 0) {
            appStr(mArgs.StringNavArgFileUrlAlt)
        } else {
            null
        }
    }
    private val mHeader by lazy { appStr(mArgs.StringNavArgHeader) }

    private val mViewModel by viewModels<OnlineFileViewModel> {
        InjectorUtils.provideOnlineViewModel(requireContext(), mFileUrl, mAlternateUrl, mHeader)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentOnlineFileBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner

            viewModel = mViewModel

            mViewModel.closeDialogEvent.observe(viewLifecycleOwner, EventObserver {
                dismiss()
            })

            mViewModel.showToastEvent.observe(viewLifecycleOwner, EventObserver {
                toast(it)
            })

            mViewModel.openUrlExternallyEvent.observe(viewLifecycleOwner, EventObserver {
                Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply {
                    startActivity(this)
                }
            })

            return this.root
        }
    }
}