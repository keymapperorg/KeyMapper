package io.github.sds100.keymapper

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.sds100.keymapper.base.actions.ChooseActionScreen
import io.github.sds100.keymapper.base.actions.ChooseActionViewModel
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementScreen
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementViewModel
import io.github.sds100.keymapper.base.constraints.ChooseConstraintScreen
import io.github.sds100.keymapper.base.constraints.ChooseConstraintViewModel
import io.github.sds100.keymapper.base.home.HomeKeyMapListScreen
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.handleRouteArgs
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.keymaps.ConfigKeyMapScreen
import io.github.sds100.keymapper.keymaps.ConfigKeyMapViewModel
import kotlinx.serialization.json.Json

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    finishActivity: () -> Unit,
) {
    val snackbarState = remember { SnackbarHostState() }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavDestination.Home,
        enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
        popEnterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
        popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
    ) {
        composable<NavDestination.Home> {
            val viewModel: HomeViewModel = hiltViewModel()

            HomeKeyMapListScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel.keyMapListViewModel,
                snackbarState = snackbarState,
                onSettingsClick = viewModel::launchSettings,
                onAboutClick = viewModel::launchAbout,
                finishActivity = finishActivity,
                fabBottomPadding = 0.dp,
            )
        }

        composable<NavDestination.NewKeyMap> { backStackEntry ->
            val viewModel: ConfigKeyMapViewModel = hiltViewModel()

            backStackEntry.handleRouteArgs<NavDestination.NewKeyMap> { args ->
                viewModel.loadNewKeyMap(groupUid = args.groupUid)

                if (args.showAdvancedTriggers) {
                    viewModel.configTriggerViewModel.showAdvancedTriggersBottomSheet = true
                }
            }

            ConfigKeyMapScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.OpenKeyMap> { backStackEntry ->
            val viewModel: ConfigKeyMapViewModel = hiltViewModel()

            backStackEntry.handleRouteArgs<NavDestination.OpenKeyMap> { args ->
                viewModel.loadKeyMap(uid = args.keyMapUid)

                if (args.showAdvancedTriggers) {
                    viewModel.configTriggerViewModel.showAdvancedTriggersBottomSheet = true
                }
            }

            ConfigKeyMapScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.ChooseAction> {
            val viewModel: ChooseActionViewModel = hiltViewModel()

            ChooseActionScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.InteractUiElement> { backStackEntry ->
            val viewModel: InteractUiElementViewModel = hiltViewModel()

            backStackEntry.handleRouteArgs<NavDestination.InteractUiElement> { destination ->
                destination.actionJson?.let { viewModel.loadAction(Json.decodeFromString(it)) }
            }

            InteractUiElementScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.ChooseConstraint> {
            val viewModel: ChooseConstraintViewModel = hiltViewModel()

            ChooseConstraintScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }
    }
}
