package io.github.sds100.keymapper.mappings.keymaps

import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.BaseEditActionFragment
import io.github.sds100.keymapper.actions.EditActionViewModel
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 12/04/2021.
 */
class EditKeyMapActionFragment : BaseEditActionFragment<KeyMap, KeyMapAction>() {

    private val configKeyMapViewModel: ConfigKeyMapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    override val viewModel: EditActionViewModel<KeyMap, KeyMapAction>
        get() = configKeyMapViewModel.editActionViewModel

    override val helpUrl: String
        get() = str(R.string.url_keymap_action_options_guide)
}