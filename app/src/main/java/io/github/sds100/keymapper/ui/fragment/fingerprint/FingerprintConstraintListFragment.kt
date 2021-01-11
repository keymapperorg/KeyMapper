package io.github.sds100.keymapper.ui.fragment.fingerprint

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ConfigFingerprintMapViewModel
import io.github.sds100.keymapper.data.viewmodel.ConstraintListViewModel
import io.github.sds100.keymapper.ui.fragment.ConstraintListFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 30/11/20.
 */
class FingerprintConstraintListFragment : ConstraintListFragment() {

    private val viewModel: ConfigFingerprintMapViewModel
        by navGraphViewModels(R.id.nav_config_fingerprint_map) {
            InjectorUtils.provideFingerprintMapListViewModel(requireContext())
        }

    override val constraintListViewModel: ConstraintListViewModel
        get() = viewModel.constraintListViewModel
}