package io.github.sds100.keymapper.actions

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
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
    setResult: (result: Bundle) -> Unit,
    navigateBack: () -> Unit
) {
    NavHost(
        navHostController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(ActionDestination.ChooseAction.NAME) { navBackStackEntry ->
            ChooseActionScreen(
                Modifier.fillMaxSize(),
                viewModel = hiltViewModel(),
                setResult = { action ->
                    setResult(bundleOf(ActionsFragment.EXTRA_ACTION to Json.encodeToString(action)))
                },
                onBack = navigateBack
            )
        }

        composable(
            ActionDestination.ConfigKeyEvent.NAME,
            ActionDestination.ConfigKeyEvent.ARGUMENTS
        ) { navBackStackEntry ->
            val keyEventAction = ActionDestination.ConfigKeyEvent.getKeyEventAction(navBackStackEntry.arguments!!)

        }
    }
}