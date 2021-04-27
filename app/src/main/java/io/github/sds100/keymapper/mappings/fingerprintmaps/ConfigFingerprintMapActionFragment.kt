package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.ConfigActionOptionsViewModel
import io.github.sds100.keymapper.mappings.OptionsBottomSheetFragment
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 27/06/2020.
 */
class ConfigFingerprintMapActionFragment : OptionsBottomSheetFragment() {

    private val configFingerprintMapViewModel: ConfigFingerprintMapViewModel by navGraphViewModels(R.id.nav_config_fingerprint_map) {
        Inject.configFingerprintMapViewModel(requireContext())
    }

    override val viewModel: ConfigActionOptionsViewModel<FingerprintMap, FingerprintMapAction>
        get() = configFingerprintMapViewModel.configActionOptionsViewModel

    override val url: String
        get() = str(R.string.url_fingerprint_action_options_guide)
}