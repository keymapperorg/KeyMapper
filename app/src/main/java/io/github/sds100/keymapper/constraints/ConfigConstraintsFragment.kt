package io.github.sds100.keymapper.constraints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.util.Inject

class ConfigConstraintsFragment : Fragment() {

    val viewModel: ConfigConstraintsViewModel by lazy {
        navGraphViewModels<ConfigKeyMapViewModel>(R.id.nav_config_keymap) {
            Inject.configKeyMapViewModel(requireContext())
        }.value.configConstraintsViewModel
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
                        ConstraintsScreen(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel,
                        )
                    }
                }
            }
            return this.root
        }
    }
}
