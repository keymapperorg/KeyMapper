package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.BaseEditActionFragment
import io.github.sds100.keymapper.mappings.EditActionViewModel
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 27/06/2020.
 */
class EditFingerprintMapActionFragment : BaseEditActionFragment<FingerprintMap, FingerprintMapAction>() {

    private val configFingerprintMapViewModel: ConfigFingerprintMapViewModel by navGraphViewModels(R.id.nav_config_fingerprint_map) {
        Inject.configFingerprintMapViewModel(requireContext())
    }

    override val viewModel: EditActionViewModel<FingerprintMap, FingerprintMapAction>
        get() = configFingerprintMapViewModel.editActionViewModel

    override val helpUrl: String
        get() = str(R.string.url_fingerprint_action_options_guide)
}
