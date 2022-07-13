package io.github.sds100.keymapper.actions

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 12/07/2022.
 */

@Composable
fun ActionsNavHost(
    modifier: Modifier = Modifier,
    startDestination: String,
    navHostController: NavHostController,
    navigateUp: (result: Bundle) -> Unit
) {
    NavHost(
        navHostController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(ActionDestination.ChooseAction.NAME) { navBackStackEntry ->
            ChooseActionScreen(onActionChosen = { action ->
                navigateUp(bundleOf(ActionsFragment.EXTRA_ACTION to Json.encodeToString(action)))
            })
        }

        composable(
            ActionDestination.ConfigKeyEvent.NAME,
            ActionDestination.ConfigKeyEvent.ARGUMENTS
        ) { navBackStackEntry ->
            val keyEventAction = ActionDestination.ConfigKeyEvent.getKeyEventAction(navBackStackEntry.arguments!!)

        }
    }
}