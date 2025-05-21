package io.github.sds100.keymapper.keymaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.base.utils.navigation.setupNavigation
import io.github.sds100.keymapper.base.utils.ui.showPopups

@AndroidEntryPoint
class ConfigKeyMapFragment : Fragment() {

    private val args by navArgs<ConfigKeyMapFragmentArgs>()

    val viewModel: ConfigKeyMapViewModel by activityViewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentComposeBinding.inflate(inflater, container, false).apply {
            composeView.apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    KeyMapperTheme {
                        ConfigKeyMapScreen(
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                                        .add(WindowInsets.displayCutout.only(sides = WindowInsetsSides.Horizontal)),
                                )
                                .fillMaxSize(),
                            viewModel = viewModel,
                            navigateBack = findNavController()::navigateUp,
                        )
                    }
                }
            }
            return this.root
        }
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
