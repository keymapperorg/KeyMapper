package io.github.sds100.keymapper.trigger

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.trigger.BaseTriggerScreen
import io.github.sds100.keymapper.base.trigger.HandleTriggerSetupBottomSheet
import io.github.sds100.keymapper.base.trigger.TriggerDiscoverBottomSheet
import io.github.sds100.keymapper.base.trigger.TriggerDiscoverScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(modifier: Modifier = Modifier, viewModel: ConfigTriggerViewModel) {
    val showFingerprintGestures: Boolean by
        viewModel.showFingerprintGesturesShortcut.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    HandleTriggerSetupBottomSheet(viewModel, sheetState)

    if (viewModel.showDiscoverTriggersBottomSheet) {
        TriggerDiscoverBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                viewModel.showDiscoverTriggersBottomSheet = false
            },
            content = {
                TriggerDiscoverScreen(
                    showFingerprintGestures = showFingerprintGestures,
                    onShortcutClick = { shortcut ->
                        scope.launch {
                            sheetState.hide()
                            viewModel.showDiscoverTriggersBottomSheet = false
                            viewModel.showTriggerSetup(shortcut)
                        }
                    },
                )
            },
        )
    }

    BaseTriggerScreen(
        modifier = modifier,
        viewModel = viewModel,
        sheetState = sheetState,
        discoverScreenContent = {
            TriggerDiscoverScreen(
                showFingerprintGestures = showFingerprintGestures,
                onShortcutClick = viewModel::showTriggerSetup,
            )
        },
    )
}
