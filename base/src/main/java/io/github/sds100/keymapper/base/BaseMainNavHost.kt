package io.github.sds100.keymapper.base

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.sds100.keymapper.base.actions.ChooseActionScreen
import io.github.sds100.keymapper.base.actions.ChooseActionViewModel
import io.github.sds100.keymapper.base.actions.ConfigCreateNotificationViewModel
import io.github.sds100.keymapper.base.actions.ConfigShellCommandViewModel
import io.github.sds100.keymapper.base.actions.CreateNotificationActionScreen
import io.github.sds100.keymapper.base.actions.ShellCommandActionScreen
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementScreen
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementViewModel
import io.github.sds100.keymapper.base.constraints.ChooseConstraintScreen
import io.github.sds100.keymapper.base.constraints.ChooseConstraintViewModel
import io.github.sds100.keymapper.base.logging.LogScreen
import io.github.sds100.keymapper.base.onboarding.HandleAccessibilityServiceDialogs
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegateImpl
import io.github.sds100.keymapper.base.promode.ProModeScreen
import io.github.sds100.keymapper.base.promode.ProModeSetupScreen
import io.github.sds100.keymapper.base.settings.AutomaticChangeImeSettingsScreen
import io.github.sds100.keymapper.base.settings.DefaultOptionsSettingsScreen
import io.github.sds100.keymapper.base.settings.SettingsScreen
import io.github.sds100.keymapper.base.settings.SettingsViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.handleRouteArgs
import kotlinx.serialization.json.Json

@Composable
fun BaseMainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegateImpl,
    composableDestinations: NavGraphBuilder.() -> Unit = {},
) {
    HandleAccessibilityServiceDialogs(setupAccessibilityServiceDelegate)

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavDestination.Home,
        enterTransition = {
            slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popExitTransition = {
            slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right)
        },
    ) {
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

        composable<NavDestination.ConfigShellCommand> { backStackEntry ->
            val viewModel: ConfigShellCommandViewModel = hiltViewModel()

            backStackEntry.handleRouteArgs<NavDestination.ConfigShellCommand> { destination ->
                destination.actionJson?.let { viewModel.loadAction(Json.decodeFromString(it)) }
            }

            ShellCommandActionScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.ConfigCreateNotification> { backStackEntry ->
            val viewModel: ConfigCreateNotificationViewModel = hiltViewModel()

            backStackEntry.handleRouteArgs<NavDestination.ConfigCreateNotification> { destination ->
                destination.actionJson?.let { viewModel.loadAction(Json.decodeFromString(it)) }
            }

            CreateNotificationActionScreen(
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

        composable<NavDestination.ChooseAction> {
            val viewModel: ChooseActionViewModel = hiltViewModel()

            ChooseActionScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()

            SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.DefaultOptionsSettings> {
            val viewModel: SettingsViewModel = hiltViewModel()

            DefaultOptionsSettingsScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.AutomaticChangeImeSettings> {
            val viewModel: SettingsViewModel = hiltViewModel()

            AutomaticChangeImeSettingsScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composable<NavDestination.ProMode> {
            ProModeScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(sides = WindowInsetsSides.Horizontal)
                            .add(
                                WindowInsets.displayCutout.only(
                                    sides = WindowInsetsSides.Horizontal,
                                ),
                            ),
                    ),
                viewModel = hiltViewModel(),
            )
        }

        composable<NavDestination.ProModeSetup> {
            ProModeSetupScreen(
                viewModel = hiltViewModel(),
            )
        }

        composable<NavDestination.Log> {
            LogScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = hiltViewModel(),
                onBackClick = { navController.popBackStack() },
            )
        }

        composableDestinations()
    }
}
