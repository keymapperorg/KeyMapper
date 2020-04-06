package io.github.sds100.keymapper.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.OnlineFileViewModel
import io.github.sds100.keymapper.databinding.FragmentOnlineFileBinding
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.result.SSLHandshakeError
import io.github.sds100.keymapper.util.result.onFailure
import io.github.sds100.keymapper.util.result.onSuccess
import splitties.resources.appStr
import splitties.toast.toast

class OnlineFileFragment : BottomSheetDialogFragment() {

    private val mArgs by navArgs<OnlineFileFragmentArgs>()

    private val mViewModel by viewModels<OnlineFileViewModel> {
        InjectorUtils.provideOnlineViewModel(requireContext(), appStr(mArgs.StringNavArgFileUrl))
    }

    private val mAlternateUrl by lazy { mArgs.StringNavArgFileUrlAlt }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentOnlineFileBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            progressCallback = mViewModel

            mViewModel.refreshIfFailed()

            mViewModel.markdownText.observe(viewLifecycleOwner) { result ->
                result.onSuccess {
                    markdown = it
                }.onFailure {

                    if (it is SSLHandshakeError) {
                        if (mAlternateUrl != 0) {
                            Intent(Intent.ACTION_VIEW, Uri.parse(appStr(mAlternateUrl))).apply {
                                startActivity(this)
                            }
                        }
                    }

                    toast(it.fullMessage)

                    dismiss()
                }
            }

            header = appStr(mArgs.StringNavArgHeader)

            return this.root
        }
    }
}