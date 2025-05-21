package io.github.sds100.keymapper.keymaps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.actions.ActionsScreen
import io.github.sds100.keymapper.base.constraints.ConstraintsScreen
import io.github.sds100.keymapper.base.keymaps.BaseConfigKeyMapScreen
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.base.keymaps.KeyMapOptionsScreen
import io.github.sds100.keymapper.base.trigger.TriggerScreen
import io.github.sds100.keymapper.base.utils.ui.UnsavedChangesDialog

@Composable
fun ConfigKeyMapScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigKeyMapViewModel,
    navigateBack: () -> Unit,
) {
    val isKeyMapEnabled by viewModel.isEnabled.collectAsStateWithLifecycle()
    val showActionTapTarget by viewModel.showActionsTapTarget.collectAsStateWithLifecycle()
    val showConstraintTapTarget by viewModel.showConstraintsTapTarget.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showBackDialog by rememberSaveable { mutableStateOf(false) }

    if (showBackDialog) {
        UnsavedChangesDialog(
            onDismiss = { showBackDialog = false },
            onDiscardClick = {
                showBackDialog = false
                navigateBack()
            },
        )
    }

    BaseConfigKeyMapScreen(
        modifier = modifier,
        isKeyMapEnabled = isKeyMapEnabled,
        onKeyMapEnabledChange = viewModel::onEnabledChanged,
        triggerScreen = {
            TriggerScreen(Modifier.fillMaxSize(), viewModel.configTriggerViewModel)
        },
        actionScreen = {
            ActionsScreen(Modifier.fillMaxSize(), viewModel.configActionsViewModel)
        },
        constraintsScreen = {
            ConstraintsScreen(
                Modifier.fillMaxSize(),
                viewModel.configConstraintsViewModel,
                snackbarHostState,
            )
        },
        optionsScreen = {
            KeyMapOptionsScreen(
                Modifier.fillMaxSize(),
                viewModel.configTriggerViewModel.optionsViewModel,
            )
        },
        onBackClick = {
            if (viewModel.isKeyMapEdited) {
                showBackDialog = true
            } else {
                navigateBack()
            }
        },
        onDoneClick = {
            viewModel.save()
            navigateBack()
        },
        snackbarHostState = snackbarHostState,
        showActionTapTarget = showActionTapTarget,
        onActionTapTargetCompleted = viewModel::onActionTapTargetCompleted,
        showConstraintTapTarget = showConstraintTapTarget,
        onConstraintTapTargetCompleted = viewModel::onConstraintTapTargetCompleted,
        onSkipTutorialClick = viewModel::onSkipTutorialClick,
    )
}
