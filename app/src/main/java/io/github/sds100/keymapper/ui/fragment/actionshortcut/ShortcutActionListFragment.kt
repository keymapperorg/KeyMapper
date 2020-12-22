package io.github.sds100.keymapper.ui.fragment.actionshortcut

import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.options.ActionShortcutOptions
import io.github.sds100.keymapper.data.viewmodel.ActionListViewModel
import io.github.sds100.keymapper.data.viewmodel.CreateActionShortcutViewModel
import io.github.sds100.keymapper.ui.fragment.ActionListFragment
import io.github.sds100.keymapper.ui.fragment.CreateActionShortcutFragmentDirections
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/11/20.
 */

class ShortcutActionListFragment : ActionListFragment<ActionShortcutOptions>() {
    private val mViewModel: CreateActionShortcutViewModel by navGraphViewModels(R.id.nav_action_shortcut) {
        InjectorUtils.provideCreateActionShortcutViewModel(requireContext())
    }

    override val actionListViewModel: ActionListViewModel<ActionShortcutOptions>
        get() = mViewModel.actionListViewModel

    override fun openActionOptionsFragment(options: ActionShortcutOptions) {
        val direction = CreateActionShortcutFragmentDirections.actionToActionOptionsFragment(options)
        findNavController().navigate(direction)
    }
}