package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ConfigActionsFragment
import io.github.sds100.keymapper.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.util.FragmentInfo

/**
 * Created by sds100 on 22/11/20.
 */

class FingerprintConfigActionsFragment : ConfigActionsFragment<FingerprintMapAction>() {

    class Info : FragmentInfo(
        R.string.action_list_header,
        R.string.url_action_guide,
        { FingerprintConfigActionsFragment() }
    )

    override var isAppBarVisible = false

    private val viewModel: ConfigFingerprintMapViewModel by hiltNavGraphViewModels(R.id.nav_config_fingerprint_map)

    override val configActionsViewModel: ConfigActionsViewModel<FingerprintMapAction, FingerprintMap>
        get() = viewModel.configActionsViewModel
}