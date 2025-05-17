package io.github.sds100.keymapper.base.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import io.github.sds100.keymapper.ActivityViewModel
import io.github.sds100.keymapper.BaseMainActivity
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.base.utils.Inject
import io.github.sds100.keymapper.base.utils.ui.setupNavigation
import io.github.sds100.keymapper.base.utils.ui.showPopups

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        Inject.homeViewModel(requireContext())
    }

    val activityViewModel: ActivityViewModel by activityViewModels<ActivityViewModel> {
        ActivityViewModel.Factory(ServiceLocator.resourceProvider(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        homeViewModel.setupNavigation(this)
        homeViewModel.keyMapListViewModel.setupNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val startDestination =
            if (!activityViewModel.handledActivityLaunchIntent &&
                requireActivity().intent?.action == BaseMainActivity.ACTION_USE_FLOATING_BUTTONS
            ) {
                activityViewModel.handledActivityLaunchIntent = true
                HomeDestination.FloatingButtons
            } else {
                HomeDestination.KeyMaps
            }

        FragmentComposeBinding.inflate(inflater, container, false).apply {
            composeView.apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    KeyMapperTheme {
                        HomeScreen(
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                                        .add(WindowInsets.displayCutout.only(sides = WindowInsetsSides.Horizontal)),
                                ),
                            viewModel = homeViewModel,
                            onSettingsClick = {
                                findNavController().navigate(NavAppDirections.toSettingsFragment())
                            },
                            onAboutClick = {
                                findNavController().navigate(NavAppDirections.actionGlobalAboutFragment())
                            },
                            finishActivity = requireActivity()::finish,
                            startDestination = startDestination,
                        )
                    }
                }
            }
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.showPopups(this, view)
        homeViewModel.keyMapListViewModel.showPopups(this, view)
        homeViewModel.listFloatingLayoutsViewModel.showPopups(this, view)
    }
}
