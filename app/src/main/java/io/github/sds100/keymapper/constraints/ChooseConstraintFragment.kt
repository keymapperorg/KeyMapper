package io.github.sds100.keymapper.constraints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import io.github.sds100.keymapper.util.viewLifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChooseConstraintFragment : Fragment() {

    companion object {
        const val EXTRA_CONSTRAINT = "extra_constraint"
    }

    private val navArgs by navArgs<ChooseConstraintFragmentArgs>()

    private val viewModel: ChooseConstraintViewModel by viewModels {
        Inject.chooseConstraintListViewModel(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setupNavigation(this)

        launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.returnResult.collectLatest { constraint ->
                viewLifecycleScope.launch {
                    withStateAtLeast(Lifecycle.State.RESUMED) {
                        setFragmentResult(
                            navArgs.requestKey,
                            bundleOf(EXTRA_CONSTRAINT to Json.encodeToString(constraint)),
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
                        ChooseConstraintScreen(
                            modifier = Modifier.fillMaxSize(),
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
