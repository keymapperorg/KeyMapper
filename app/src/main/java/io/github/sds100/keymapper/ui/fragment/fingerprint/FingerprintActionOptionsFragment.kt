package io.github.sds100.keymapper.ui.fragment.fingerprint

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyControllerAdapter
import io.github.sds100.keymapper.data.model.options.FingerprintActionOptions
import io.github.sds100.keymapper.data.viewmodel.FingerprintActionOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentFingerprintActionOptionsBinding
import io.github.sds100.keymapper.ui.fragment.BaseOptionsDialogFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 27/06/2020.
 */
class FingerprintActionOptionsFragment
    : BaseOptionsDialogFragment<FragmentFingerprintActionOptionsBinding,
    FingerprintActionOptions>() {

    companion object {
        const val REQUEST_KEY = "request_choose_action_options"
    }

    override val optionsViewModel: FingerprintActionOptionsViewModel by viewModels {
        InjectorUtils.provideFingerprintActionOptionsViewModel()
    }

    override val requestKey = REQUEST_KEY

    override val initialOptions: FingerprintActionOptions
        get() = navArgs<FingerprintActionOptionsFragmentArgs>().value
            .StringNavArgFingerprintActionOptions

    override fun subscribeCustomUi(binding: FragmentFingerprintActionOptionsBinding) {
        binding.apply {
            viewModel = optionsViewModel
        }
    }

    override fun setRecyclerViewAdapter(
        binding: FragmentFingerprintActionOptionsBinding,
        adapter: EpoxyControllerAdapter
    ) {
        binding.epoxyRecyclerView.adapter = adapter
    }

    override fun bind(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFingerprintActionOptionsBinding {
        return FragmentFingerprintActionOptionsBinding.inflate(inflater, container, false)
    }
}