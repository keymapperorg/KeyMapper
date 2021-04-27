package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.OptionsBottomSheetFragment
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 12/04/2021.
 */
class ConfigTriggerKeyFragment : OptionsBottomSheetFragment() {

    private val configKeyMapViewModel: ConfigKeyMapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        Inject.configKeyMapViewModel(requireContext())
    }

    override val viewModel: ConfigTriggerKeyViewModel
        get() = configKeyMapViewModel.configTriggerKeyViewModel

    override val url: String
        get() = str(R.string.url_trigger_key_options_guide)
}