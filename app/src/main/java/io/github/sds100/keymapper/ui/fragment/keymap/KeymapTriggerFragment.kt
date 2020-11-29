package io.github.sds100.keymapper.ui.fragment.keymap

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.TriggerViewModel
import io.github.sds100.keymapper.ui.fragment.TriggerFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/11/20.
 */

class KeymapTriggerFragment(private val mKeymapId: Long) : TriggerFragment() {
    private val mConfigKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideNewConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    override val triggerViewModel: TriggerViewModel
        get() = mConfigKeymapViewModel.triggerViewModel
}