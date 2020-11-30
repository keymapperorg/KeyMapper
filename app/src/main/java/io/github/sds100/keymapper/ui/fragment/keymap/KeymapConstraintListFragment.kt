package io.github.sds100.keymapper.ui.fragment.keymap

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.ConstraintListViewModel
import io.github.sds100.keymapper.ui.fragment.ConstraintListFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 30/11/20.
 */
class KeymapConstraintListFragment(private val mKeymapId: Long) : ConstraintListFragment() {

    private val mConfigKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideNewConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    override val constraintListViewModel: ConstraintListViewModel
        get() = mConfigKeymapViewModel.constraintListViewModel
}