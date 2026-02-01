package io.github.sds100.keymapper

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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.composable
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.BaseMainNavHost
import io.github.sds100.keymapper.base.actions.ActionsScreen
import io.github.sds100.keymapper.base.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.base.constraints.ConstraintsScreen
import io.github.sds100.keymapper.base.home.HomeKeyMapListScreen
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapScreen
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.base.keymaps.KeyMapOptionsScreen
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegateImpl
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProviderImpl
import io.github.sds100.keymapper.base.utils.navigation.SetupNavigation
import io.github.sds100.keymapper.base.utils.navigation.handleRouteArgs
import io.github.sds100.keymapper.base.utils.navigation.setupFragmentNavigation
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.trigger.AdvancedTriggersScreenFoss
import io.github.sds100.keymapper.trigger.ConfigTriggerViewModel
import io.github.sds100.keymapper.trigger.TriggerScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var navigationProvider: NavigationProviderImpl

    @Inject
    lateinit var setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegateImpl

    private lateinit var composeView: ComposeView

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationProvider.setupFragmentNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).also {
            composeView = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = NavHostController(requireContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())

            if (savedInstanceState == null) {
                restoreState(navigationProvider.savedState)
                navigationProvider.savedState = null
            } else {
                restoreState(savedInstanceState)
            }
        }

        composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SetupNavigation(navigationProvider, navController!!)

                KeyMapperTheme {
                    BaseMainNavHost(
                        modifier = Modifier
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                                    .add(
                                        WindowInsets.displayCutout.only(
                                            sides = WindowInsetsSides.Horizontal,
                                        ),
                                    ),
                            ),
                        navController = navController!!,
                        setupAccessibilityServiceDelegate = setupAccessibilityServiceDelegate,
                        composableDestinations = {
                            composableDestinations(navController!!)
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navController?.saveState()?.let(outState::putAll)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        // onSaveInstanceState is only called when the activity's onSaveInstanceState method
        // is called so use our own place to save the navigation state
        navigationProvider.savedState = navController?.saveState()

        super.onDestroyView()
    }

    private fun NavGraphBuilder.composableDestinations(navController: NavController) {
        composable<NavDestination.Home> {
            val snackbarState = remember { SnackbarHostState() }

            val viewModel: HomeViewModel = hiltViewModel()

            HomeKeyMapListScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel.keyMapListViewModel,
                snackbarState = snackbarState,
                onSettingsClick = viewModel::launchSettings,
                onAboutClick = viewModel::launchAbout,
                finishActivity = {
                    requireActivity().finish()
                },
                fabBottomPadding = 0.dp,
            )
        }

        composable<NavDestination.NewKeyMap> { backStackEntry ->
            val keyMapViewModel: ConfigKeyMapViewModel = hiltViewModel()
            val triggerViewModel: ConfigTriggerViewModel = hiltViewModel()
            val actionsViewModel: ConfigActionsViewModel = hiltViewModel()
            val constraintsViewModel: ConfigConstraintsViewModel = hiltViewModel()
            val snackbarHostState = remember { SnackbarHostState() }

            backStackEntry.handleRouteArgs<NavDestination.NewKeyMap> { args ->
                keyMapViewModel.loadNewKeyMap(groupUid = args.groupUid)
                args.triggerSetupShortcut?.let { triggerViewModel.showTriggerSetup(it) }
            }

            ConfigKeyMapScreen(
                modifier = Modifier.fillMaxSize(),
                snackbarHostState = snackbarHostState,
                keyMapViewModel = keyMapViewModel,
                triggerScreen = {
                    TriggerScreen(Modifier.fillMaxSize(), triggerViewModel)
                },
                actionsScreen = {
                    ActionsScreen(Modifier.fillMaxSize(), actionsViewModel)
                },
                constraintsScreen = {
                    ConstraintsScreen(
                        Modifier.fillMaxSize(),
                        constraintsViewModel,
                        snackbarHostState,
                    )
                },
                optionsScreen = {
                    KeyMapOptionsScreen(
                        Modifier.fillMaxSize(),
                        triggerViewModel.optionsViewModel,
                    )
                },
            )
        }

        composable<NavDestination.AdvancedTriggers> {
            AdvancedTriggersScreenFoss(
                modifier = Modifier.fillMaxSize(),
                onBack = navController::popBackStack,
            )
        }
        composable<NavDestination.OpenKeyMap> { backStackEntry ->
            val keyMapViewModel: ConfigKeyMapViewModel = hiltViewModel()
            val triggerViewModel: ConfigTriggerViewModel = hiltViewModel()
            val actionsViewModel: ConfigActionsViewModel = hiltViewModel()
            val constraintsViewModel: ConfigConstraintsViewModel = hiltViewModel()
            val snackbarHostState = remember { SnackbarHostState() }

            backStackEntry.handleRouteArgs<NavDestination.OpenKeyMap> { args ->
                keyMapViewModel.loadKeyMap(uid = args.keyMapUid)
            }

            ConfigKeyMapScreen(
                modifier = Modifier.fillMaxSize(),
                snackbarHostState = snackbarHostState,
                keyMapViewModel = keyMapViewModel,
                triggerScreen = {
                    TriggerScreen(Modifier.fillMaxSize(), triggerViewModel)
                },
                actionsScreen = {
                    ActionsScreen(Modifier.fillMaxSize(), actionsViewModel)
                },
                constraintsScreen = {
                    ConstraintsScreen(
                        Modifier.fillMaxSize(),
                        constraintsViewModel,
                        snackbarHostState,
                    )
                },
                optionsScreen = {
                    KeyMapOptionsScreen(
                        Modifier.fillMaxSize(),
                        triggerViewModel.optionsViewModel,
                    )
                },
            )
        }
    }
}
