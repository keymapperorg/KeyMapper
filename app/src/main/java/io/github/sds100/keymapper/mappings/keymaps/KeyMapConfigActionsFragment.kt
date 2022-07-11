package io.github.sds100.keymapper.mappings.keymaps

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ConfigActionsFragment
import io.github.sds100.keymapper.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.util.FragmentInfo

/**
 * Created by sds100 on 22/11/20.
 */

class KeyMapConfigActionsFragment : ConfigActionsFragment<KeyMapAction>() {

    class Info : FragmentInfo(
        R.string.action_list_header,
        R.string.url_action_guide,
        { KeyMapConfigActionsFragment() }
    )

    override var isAppBarVisible = false

    private val configKeyMapViewModel: ConfigKeyMapViewModel by hiltNavGraphViewModels(R.id.nav_config_keymap)

    override val configActionsViewModel: ConfigActionsViewModel<KeyMapAction, KeyMap>
        get() = configKeyMapViewModel.configActionsViewModel
}