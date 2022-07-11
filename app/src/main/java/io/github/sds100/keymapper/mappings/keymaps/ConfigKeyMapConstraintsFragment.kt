package io.github.sds100.keymapper.mappings.keymaps

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
class ConfigKeyMapConstraintsFragment : ConfigConstraintsFragment() {
    class Info : FragmentInfo(
        R.string.constraint_list_header,
        R.string.url_constraints_guide,
        { ConfigKeyMapConstraintsFragment() }
    )

    override var isAppBarVisible = false

    private val configKeyMapViewModel: ConfigKeyMapViewModel by hiltNavGraphViewModels(R.id.nav_config_keymap)

    override val configConstraintsViewModel: ConfigConstraintsViewModel
        get() = configKeyMapViewModel.configConstraintsViewModel
}