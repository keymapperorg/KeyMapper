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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.BaseMainNavHost
import io.github.sds100.keymapper.base.actions.ActionsScreen
import io.github.sds100.keymapper.base.actions.ChooseActionScreen
import io.github.sds100.keymapper.base.actions.ChooseActionViewModel
import io.github.sds100.keymapper.base.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.base.constraints.ConstraintsScreen
import io.github.sds100.keymapper.base.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.base.home.HomeKeyMapListScreen
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapScreen
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.base.keymaps.KeyMapOptionsScreen
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProviderImpl
import io.github.sds100.keymapper.base.utils.navigation.SetupNavigation
import io.github.sds100.keymapper.base.utils.navigation.handleRouteArgs
import io.github.sds100.keymapper.base.utils.navigation.setupFragmentNavigation
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.trigger.ConfigTriggerViewModel
import io.github.sds100.keymapper.trigger.TriggerScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var navigationProvider: NavigationProviderImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationProvider.setupFragmentNavigation(this)
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
                    val navController = rememberNavController()
                    SetupNavigation(navigationProvider, navController)

                    KeyMapperTheme {
                        BaseMainNavHost(
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                                        .add(WindowInsets.displayCutout.only(sides = WindowInsetsSides.Horizontal)),
                                ),
                            navController = navController,
                            composableDestinations = {
                                composableDestinations()
                            },
                        )
                    }
                }
            }
            return this.root
        }
    }

    private fun NavGraphBuilder.composableDestinations() {
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

                if (args.showAdvancedTriggers) {
                    triggerViewModel.showAdvancedTriggersBottomSheet = true
                }
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

        composable<NavDestination.OpenKeyMap> { backStackEntry ->
            val keyMapViewModel: ConfigKeyMapViewModel = hiltViewModel()
            val triggerViewModel: ConfigTriggerViewModel = hiltViewModel()
            val actionsViewModel: ConfigActionsViewModel = hiltViewModel()
            val constraintsViewModel: ConfigConstraintsViewModel = hiltViewModel()
            val snackbarHostState = remember { SnackbarHostState() }

            backStackEntry.handleRouteArgs<NavDestination.OpenKeyMap> { args ->
                keyMapViewModel.loadKeyMap(uid = args.keyMapUid)

                if (args.showAdvancedTriggers) {
                    triggerViewModel.showAdvancedTriggersBottomSheet = true
                }
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

        composable<NavDestination.ChooseAction> {
            val viewModel: ChooseActionViewModel = hiltViewModel()

            ChooseActionScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }
    }
}
