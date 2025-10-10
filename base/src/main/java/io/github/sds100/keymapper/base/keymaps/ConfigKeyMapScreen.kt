package io.github.sds100.keymapper.base.keymaps

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.utils.ui.UnsavedChangesDialog

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
    val isKeyMapEnabled by keyMapViewModel.isEnabled.collectAsStateWithLifecycle()
    val showActionTapTarget by keyMapViewModel.showActionsTapTarget.collectAsStateWithLifecycle()
    var showBackDialog by rememberSaveable { mutableStateOf(false) }

    if (showBackDialog) {
        UnsavedChangesDialog(
            onDismiss = { showBackDialog = false },
            onDiscardClick = {
                showBackDialog = false
                keyMapViewModel.onBackClick()
            },
        )
    }

    BaseConfigKeyMapScreen(
        modifier = modifier,
        isKeyMapEnabled = isKeyMapEnabled,
        onKeyMapEnabledChange = keyMapViewModel::onEnabledChanged,
        triggerScreen = triggerScreen,
        actionsScreen = actionsScreen,
        constraintsScreen = constraintsScreen,
        optionsScreen = optionsScreen,
        onBackClick = {
            if (keyMapViewModel.isKeyMapEdited) {
                showBackDialog = true
            } else {
                keyMapViewModel.onBackClick()
            }
        },
        onDoneClick = keyMapViewModel::onDoneClick,
        snackbarHostState = snackbarHostState,
        showActionTapTarget = showActionTapTarget,
        onActionTapTargetCompleted = keyMapViewModel::onActionTapTargetCompleted,
        onSkipTutorialClick = keyMapViewModel::onSkipTutorialClick,
    )
}
