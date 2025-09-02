package io.github.sds100.keymapper.base

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementScreen
import io.github.sds100.keymapper.base.actions.uielement.InteractUiElementViewModel
import io.github.sds100.keymapper.base.constraints.ChooseConstraintScreen
import io.github.sds100.keymapper.base.constraints.ChooseConstraintViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.handleRouteArgs
import kotlinx.serialization.json.Json

@Composable
fun BaseMainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    composableDestinations: NavGraphBuilder.() -> Unit = {},
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavDestination.Home,
        enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
        popEnterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
        popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
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

        composable<NavDestination.ChooseConstraint> {
            val viewModel: ChooseConstraintViewModel = hiltViewModel()

            ChooseConstraintScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
            )
        }

        composableDestinations()
    }
}
