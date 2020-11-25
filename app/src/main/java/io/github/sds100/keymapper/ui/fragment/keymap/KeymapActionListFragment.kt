package io.github.sds100.keymapper.ui.fragment.keymap

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ActionListViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.ui.fragment.ActionListFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/11/20.
 */

class KeymapActionListFragment(private val mKeymapId: Long) : ActionListFragment() {
    private val mConfigKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_new_config_keymap) {
        InjectorUtils.provideNewConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    override val actionListViewModel: ActionListViewModel
        get() = mConfigKeymapViewModel.actionListViewModel
}