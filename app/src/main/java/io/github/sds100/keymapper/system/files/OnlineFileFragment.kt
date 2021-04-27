package io.github.sds100.keymapper.system.files

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
import io.github.sds100.keymapper.databinding.FragmentOnlineFileBinding
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.getFullMessage
import splitties.toast.toast

class OnlineFileFragment : BottomSheetDialogFragment() {

    private val args by navArgs<OnlineFileFragmentArgs>()

    private val fileUrl by lazy { str(args.StringNavArgFileUrl) }
    private val alternateUrl by lazy {
        if (args.StringNavArgFileUrlAlt != 0) {
            str(args.StringNavArgFileUrlAlt)
        } else {
            null
        }
    }
    private val header by lazy { str(args.StringNavArgHeader) }

    private val viewModel by viewModels<OnlineFileViewModel> {
        Inject.onlineFileViewModel(requireContext(), fileUrl, alternateUrl, header)
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
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.viewModel = viewModel

        viewModel.eventStream.observe(viewLifecycleOwner, {
            when (it) {
//                is CloseDialog -> dismiss()
//                is ShowErrorMessage -> toast(it.error.getFullMessage(requireContext()))
//                is OpenUrl -> {
//                    Intent(Intent.ACTION_VIEW, Uri.parse(it.url)).apply {
//                        startActivity(this)
//                    }
//                }
            }
        })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}