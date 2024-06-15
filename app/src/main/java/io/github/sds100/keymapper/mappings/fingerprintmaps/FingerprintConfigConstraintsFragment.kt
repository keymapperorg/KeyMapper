package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.constraints.ConfigConstraintsFragment
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.Inject

/**
 * Created by sds100 on 30/11/20.
 */
class FingerprintConfigConstraintsFragment : ConfigConstraintsFragment() {

    class Info :
        FragmentInfo(
            R.string.constraint_list_header,
            R.string.url_constraints_guide,
            { FingerprintConfigConstraintsFragment() },
        )

    override var isAppBarVisible = false

    private val viewModel: ConfigFingerprintMapViewModel
        by navGraphViewModels(R.id.nav_config_fingerprint_map) {
            Inject.configFingerprintMapViewModel(requireContext())
        }

    override val configConstraintsViewModel: ConfigConstraintsViewModel
        get() = viewModel.configConstraintsViewModel
}
