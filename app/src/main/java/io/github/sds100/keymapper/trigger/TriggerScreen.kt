package io.github.sds100.keymapper.trigger

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sds100.keymapper.base.trigger.BaseTriggerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(modifier: Modifier = Modifier, viewModel: ConfigTriggerViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (viewModel.showAdvancedTriggersBottomSheet) {
        AdvancedTriggersBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            viewModel = viewModel,
            onDismissRequest = {
                viewModel.showAdvancedTriggersBottomSheet = false
            },
            sheetState = sheetState,
        )
    }

    BaseTriggerScreen(modifier, viewModel)
}
