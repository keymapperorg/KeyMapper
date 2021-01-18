package io.github.sds100.keymapper.ui.fragment.keymap

import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.options.KeymapActionOptions
import io.github.sds100.keymapper.data.viewmodel.ActionListViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.ui.fragment.ActionListFragment
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/11/20.
 */

class KeymapActionListFragment : ActionListFragment<KeymapActionOptions>() {

    class Info : FragmentInfo(
        R.string.action_list_header,
        R.string.url_action_guide,
        { KeymapActionListFragment() }
    )

    override var isAppBarVisible = false

    private val configKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext())
    }

    override val actionListViewModel: ActionListViewModel<KeymapActionOptions>
        get() = configKeymapViewModel.actionListViewModel

    override fun openActionOptionsFragment(options: KeymapActionOptions) {
        val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToActionOptionsFragment(options)
        findNavController().navigate(direction)
    }
}