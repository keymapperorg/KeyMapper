package io.github.sds100.keymapper

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
import io.github.sds100.keymapper.base.home.HomeKeyMapListScreen
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.handleRoute
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.keymaps.ConfigKeyMapScreen
import io.github.sds100.keymapper.keymaps.ConfigKeyMapViewModel

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

            backStackEntry.handleRoute<NavDestination.NewKeyMap> { args ->
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

            backStackEntry.handleRoute<NavDestination.OpenKeyMap> { args ->
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
    }
}

