package io.github.sds100.keymapper.base.actions

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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withStateAtLeast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.base.utils.navigation.setupNavigation
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.ui.showPopups
import io.github.sds100.keymapper.base.utils.ui.viewLifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// TODO move into MainFragment and compose navigation
@AndroidEntryPoint
class ChooseActionFragment : Fragment() {

    companion object {
        const val EXTRA_ACTION = "extra_action"
    }

    private val args: ChooseActionFragmentArgs by navArgs<ChooseActionFragmentArgs>()

    private val viewModel by viewModels<ChooseActionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setupNavigation(this)

        launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.returnAction.collectLatest { action ->
                viewLifecycleScope.launch {
                    withStateAtLeast(Lifecycle.State.RESUMED) {
                        setFragmentResult(
                            args.requestKey,
                            bundleOf(EXTRA_ACTION to Json.encodeToString(action)),
                        )
                        findNavController().navigateUp()
                    }
                }
            }
        }
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
                        ChooseActionScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(
                                    WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                                        .add(WindowInsets.displayCutout.only(sides = WindowInsetsSides.Horizontal)),
                                ),
                            viewModel = viewModel,
                            onNavigateBack = findNavController()::navigateUp,
                        )
                    }
                }
            }
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showPopups(this, view)
    }
}
