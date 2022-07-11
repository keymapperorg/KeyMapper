package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.constraints.ConfigConstraintsFragment
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.util.FragmentInfo

/**
 * Created by sds100 on 30/11/20.
 */
@AndroidEntryPoint
class FingerprintConfigConstraintsFragment : ConfigConstraintsFragment() {

    class Info : FragmentInfo(
        R.string.constraint_list_header,
        R.string.url_constraints_guide,
        { FingerprintConfigConstraintsFragment() }
    )

    override var isAppBarVisible = false

    private val viewModel: ConfigFingerprintMapViewModel
        by hiltNavGraphViewModels(R.id.nav_config_fingerprint_map)

    override val configConstraintsViewModel: ConfigConstraintsViewModel
        get() = viewModel.configConstraintsViewModel
}