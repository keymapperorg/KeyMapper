package io.github.sds100.keymapper.base.keymaps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.setupNavigation
import io.github.sds100.keymapper.base.utils.ui.showPopups
import io.github.sds100.keymapper.keymaps.ConfigKeyMapFragmentArgs

abstract class BaseConfigKeyMapFragment : Fragment() {

    private val args by navArgs<ConfigKeyMapFragmentArgs>()

    val viewModel: ConfigKeyMapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // only load the keymap if opening this fragment for the first time
        if (savedInstanceState == null) {
            args.keyMapUid.also { keyMapUid ->
                if (keyMapUid == null) {
                    viewModel.loadNewKeymap(
                        args.newFloatingButtonTriggerKey,
                        groupUid = args.groupUid,
                    )
                } else {
                    viewModel.loadKeyMap(keyMapUid)
                }
            }

            if (args.showAdvancedTriggers) {
                viewModel.configTriggerViewModel.showAdvancedTriggersBottomSheet = true
            }
        }

        viewModel.configTriggerViewModel.setupNavigation(this)
        viewModel.configActionsViewModel.setupNavigation(this)
        viewModel.configConstraintsViewModel.setupNavigation(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.configTriggerViewModel.showPopups(this, view)
        viewModel.configTriggerViewModel.optionsViewModel.showPopups(this, view)
        viewModel.configActionsViewModel.showPopups(this, view)
        viewModel.configConstraintsViewModel.showPopups(this, view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState ?: return

        viewModel.restoreState(savedInstanceState)
    }
}
