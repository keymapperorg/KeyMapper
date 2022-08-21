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
import io.github.sds100.keymapper.actions.keyevent.ChooseKeyCodeScreen
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventScreen
import io.github.sds100.keymapper.actions.sound.ChooseSoundScreen
import io.github.sds100.keymapper.actions.tapscreen.CreateTapScreenActionScreen
import io.github.sds100.keymapper.destinations.*
import io.github.sds100.keymapper.system.apps.ChooseActivityScreen
import io.github.sds100.keymapper.system.apps.ChooseAppScreen
import io.github.sds100.keymapper.system.apps.ChooseAppShortcutScreen
import io.github.sds100.keymapper.system.intents.ConfigIntentScreen
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
                appResultRecipient = resultRecipient(),
                appShortcutResultRecipient = resultRecipient(),
                tapScreenActionResultRecipient = resultRecipient(),
                chooseSoundResultRecipient = resultRecipient(),
                configKeyEventResultRecipient = resultRecipient(),
                configIntentResultRecipient = resultRecipient(),
                setResult = { setResult(bundleOf(ActionsFragment.EXTRA_ACTION to Json.encodeToString(it))) },
                navigateBack = navigateBack
            )
        }

        composable(ChooseKeyCodeScreenDestination) {
            ChooseKeyCodeScreen(
                viewModel = hiltViewModel(),
                resultNavigator = resultBackNavigator(navHostController)
            )
        }

        composable(ChooseAppScreenDestination) {
            ChooseAppScreen(
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator(navHostController)
            )
        }

        composable(ChooseAppShortcutScreenDestination) {
            ChooseAppShortcutScreen(
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator(navHostController)
            )
        }

        composable(CreateTapScreenActionScreenDestination) {
            CreateTapScreenActionScreen(
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator(navHostController)
            )
        }

        composable(ChooseSoundScreenDestination) {
            ChooseSoundScreen(
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator(navHostController)
            )
        }

        composable(ConfigKeyEventScreenDestination) {
            ConfigKeyEventScreen(
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator(navHostController),
                navigator = destinationsNavigator(navHostController),
                keyCodeResultRecipient = resultRecipient()
            )
        }

        composable(ConfigIntentScreenDestination) {
            ConfigIntentScreen(
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator(navHostController),
                navigator = destinationsNavigator(navHostController),
                activityResultRecipient = resultRecipient()
            )
        }

        composable(ChooseActivityScreenDestination) {
            ChooseActivityScreen(
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator(navHostController))
        }
    }
}