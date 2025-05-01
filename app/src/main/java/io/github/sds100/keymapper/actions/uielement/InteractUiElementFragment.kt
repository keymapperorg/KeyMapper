package io.github.sds100.keymapper.actions.uielement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withStateAtLeast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.viewLifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class InteractUiElementFragment : Fragment() {

    companion object {
        const val EXTRA_ACTION = "extra_action"
    }

    private val args: InteractUiElementFragmentArgs by navArgs<InteractUiElementFragmentArgs>()

    private val viewModel by viewModels<InteractUiElementViewModel> {
        Inject.interactUiElementViewModel(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args.action?.let { argsAction -> viewModel.loadAction(Json.decodeFromString(argsAction)) }

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
                        InteractUiElementScreen(
                            viewModel = viewModel,
                            navigateBack = findNavController()::navigateUp,
                        )
                    }
                }
            }
            return this.root
        }
    }
}
