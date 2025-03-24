package io.github.sds100.keymapper.mappings.keymaps.trigger

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

/**
 * Created by sds100 on 25/11/20.
 */

class TriggerFragment : Fragment() {

    private val configTriggerViewModel: ConfigTriggerViewModel by lazy {
        navGraphViewModels<ConfigKeyMapViewModel>(R.id.nav_config_keymap) {
            Inject.configKeyMapViewModel(requireContext())
        }.value.configTriggerViewModel
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
                        TriggerScreen(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = configTriggerViewModel,
                        )
                    }
                }
            }
            return this.root
        }
    }
}
