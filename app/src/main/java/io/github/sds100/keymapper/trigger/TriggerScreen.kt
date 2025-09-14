package io.github.sds100.keymapper.trigger

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.trigger.BaseTriggerScreen
import io.github.sds100.keymapper.base.trigger.TriggerDiscoverScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(modifier: Modifier = Modifier, viewModel: ConfigTriggerViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showFingerprintGestures: Boolean by viewModel.showFingerprintGesturesShortcut.collectAsStateWithLifecycle()

    if (viewModel.showAdvancedTriggersBottomSheet) {
        AdvancedTriggersBottomSheet(
            viewModel = viewModel,
            onDismissRequest = {
                viewModel.showAdvancedTriggersBottomSheet = false
            },
            sheetState = sheetState,
        )
    }

    BaseTriggerScreen(modifier, viewModel, discoverScreenContent = {
        TriggerDiscoverScreen(
            showFloatingButtons = true,
            showFingerprintGestures = showFingerprintGestures,
            onShortcutClick = viewModel::showTriggerSetup,
        )
    })
}
