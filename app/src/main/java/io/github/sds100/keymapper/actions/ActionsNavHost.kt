package io.github.sds100.keymapper.actions

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.ramcosta.composedestinations.scope.resultBackNavigator
import com.ramcosta.composedestinations.scope.resultRecipient
import com.ramcosta.composedestinations.utils.composable
import io.github.sds100.keymapper.actions.destinations.ChooseActionScreenDestination
import io.github.sds100.keymapper.actions.destinations.ChooseKeyCodeScreenDestination
import io.github.sds100.keymapper.actions.keyevent.ChooseKeyCodeScreen
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
        composable(ChooseActionScreenDestination) {
            ChooseActionScreen(
                viewModel = hiltViewModel(),
                navigator = destinationsNavigator(navHostController),
                keyCodeResultRecipient = resultRecipient(),
                setResult = { setResult(bundleOf(ActionsFragment.EXTRA_ACTION to Json.encodeToString(it))) },
                navigateBack = navigateBack
            )
        }

        composable(ChooseKeyCodeScreenDestination) {
            ChooseKeyCodeScreen(
                resultNavigator = resultBackNavigator(navHostController)
            )
        }
    }
}