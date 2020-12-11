package io.github.sds100.keymapper.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.data.viewmodel.OnlineFileViewModel
import io.github.sds100.keymapper.databinding.FragmentOnlineFileBinding
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.getFullMessage
import splitties.toast.toast

class OnlineFileFragment : BottomSheetDialogFragment() {

    private val mArgs by navArgs<OnlineFileFragmentArgs>()

    private val mFileUrl by lazy { str(mArgs.StringNavArgFileUrl) }
    private val mAlternateUrl by lazy {
        if (mArgs.StringNavArgFileUrlAlt != 0) {
            str(mArgs.StringNavArgFileUrlAlt)
        } else {
            null
        }
    }
    private val mHeader by lazy { str(mArgs.StringNavArgHeader) }

    private val mViewModel by viewModels<OnlineFileViewModel> {
        InjectorUtils.provideOnlineViewModel(requireContext(), mFileUrl, mAlternateUrl, mHeader)
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentOnlineFileBinding? = null
    val binding: FragmentOnlineFileBinding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentOnlineFileBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        mViewModel.eventStream.observe(viewLifecycleOwner, {
            when (it) {
                is CloseDialog -> dismiss()
                is ShowErrorMessage -> toast(it.failure.getFullMessage(requireContext()))
                is OpenUrl -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse(it.url)).apply {
                        startActivity(this)
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}