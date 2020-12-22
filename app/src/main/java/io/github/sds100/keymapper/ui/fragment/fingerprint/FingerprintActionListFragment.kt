package io.github.sds100.keymapper.ui.fragment.fingerprint

import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.options.FingerprintActionOptions
import io.github.sds100.keymapper.data.viewmodel.ActionListViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigFingerprintMapViewModel
import io.github.sds100.keymapper.ui.fragment.ActionListFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/11/20.
 */

class FingerprintActionListFragment : ActionListFragment<FingerprintActionOptions>() {
    private val mViewModel: ConfigFingerprintMapViewModel
        by navGraphViewModels(R.id.nav_config_fingerprint_map) {
            InjectorUtils.provideFingerprintMapListViewModel(requireContext())
        }

    override val actionListViewModel: ActionListViewModel<FingerprintActionOptions>
        get() = mViewModel.actionListViewModel

    override fun openActionOptionsFragment(options: FingerprintActionOptions) {
        val direction = ConfigFingerprintMapFragmentDirections
            .actionConfigFingerprintMapFragmentToActionOptionsFragment(options)

        findNavController().navigate(direction)
    }
}