package io.github.sds100.keymapper.mappings.keymaps

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.constraints.ConfigConstraintsFragment
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.Inject

/**
 * Created by sds100 on 30/11/20.
 */
class ConfigKeyMapConstraintsFragment : ConfigConstraintsFragment() {
    class Info :
        FragmentInfo(
            R.string.constraint_list_header,
            R.string.url_constraints_guide,
            { ConfigKeyMapConstraintsFragment() },
        )

    override var isAppBarVisible = false

    private val configKeyMapViewModel: ConfigKeyMapViewModel
        by navGraphViewModels(R.id.nav_config_keymap) {
            Inject.configKeyMapViewModel(requireContext())
        }

    override val configConstraintsViewModel: ConfigConstraintsViewModel
        get() = configKeyMapViewModel.configConstraintsViewModel
}
