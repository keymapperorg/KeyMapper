package io.github.sds100.keymapper.ui.fragment.keymap

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.ConstraintListViewModel
import io.github.sds100.keymapper.ui.fragment.ConstraintListFragment
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 30/11/20.
 */
class KeymapConstraintListFragment : ConstraintListFragment() {
    class Info : FragmentInfo(
        R.string.constraint_list_header,
        R.string.url_constraints_guide,
        { KeymapConstraintListFragment() }
    )

    override var isAppBarVisible = false

    private val configKeymapViewModel: ConfigKeymapViewModel
        by navGraphViewModels(R.id.nav_config_keymap) {
            InjectorUtils.provideConfigKeymapViewModel(requireContext())
        }

    override val constraintListViewModel: ConstraintListViewModel
        get() = configKeymapViewModel.constraintListViewModel
}