package io.github.sds100.keymapper.base.keymaps

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ConfigKeyMapScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    keyMapViewModel: ConfigKeyMapViewModel,
    triggerScreen: @Composable () -> Unit,
    actionsScreen: @Composable () -> Unit,
    constraintsScreen: @Composable () -> Unit,
    optionsScreen: @Composable () -> Unit,
) {
    val state by keyMapViewModel.state.collectAsStateWithLifecycle()

    BaseConfigKeyMapScreen(
        modifier = modifier,
        state = state,
        onEditNameClick = keyMapViewModel::onEditNameClick,
        onConfirmNameClick = keyMapViewModel::onConfirmNameClick,
        onCancelEditNameClick = keyMapViewModel::onCancelEditNameClick,
        onKeyMapEnabledChange = keyMapViewModel::onEnabledChanged,
        onUndoClick = keyMapViewModel::onUndoClick,
        onRedoClick = keyMapViewModel::onRedoClick,
        triggerScreen = triggerScreen,
        actionsScreen = actionsScreen,
        constraintsScreen = constraintsScreen,
        optionsScreen = optionsScreen,
        onBackClick = keyMapViewModel::onBackClick,
        snackbarHostState = snackbarHostState,
    )
}
