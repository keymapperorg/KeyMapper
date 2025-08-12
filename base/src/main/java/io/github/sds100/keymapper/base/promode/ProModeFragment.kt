package io.github.sds100.keymapper.base.promode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.base.utils.navigation.NavDestination

// TODO delete because settings will be composable
@AndroidEntryPoint
class ProModeFragment : Fragment() {

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
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = NavDestination.ID_PRO_MODE,
                            enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left) },
                            exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
                            popEnterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
                            popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
                        ) {
                            composable(NavDestination.ID_PRO_MODE) {
                                ProModeScreen(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .windowInsetsPadding(
                                            WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                                                .add(WindowInsets.displayCutout.only(sides = WindowInsetsSides.Horizontal)),
                                        ),
                                    viewModel = hiltViewModel(),
                                    onNavigateBack = { findNavController().navigateUp() },
                                    onNavigateToSetup = { navController.navigate(NavDestination.ProModeSetup.ID_PRO_MODE_SETUP) }
                                )
                            }
                            composable(NavDestination.ProModeSetup.ID_PRO_MODE_SETUP) {
                                ProModeSetupScreen(
                                    viewModel = hiltViewModel(),
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                        }
                    }
                }
            }
            return this.root
        }
    }
}
