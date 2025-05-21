package io.github.sds100.keymapper.trigger

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sds100.keymapper.base.trigger.BaseTriggerScreen

@Composable
fun TriggerScreen(modifier: Modifier = Modifier, viewModel: ConfigTriggerViewModel) {


    // TODO
//    HandleAssistantTriggerSetupBottomSheet(viewModel = viewModel)
//
//    if (viewModel.showAdvancedTriggersBottomSheet) {
//        AdvancedTriggersBottomSheet(
//            modifier = Modifier.systemBarsPadding(),
//            viewModel = viewModel,
//            onDismissRequest = {
//                viewModel.showAdvancedTriggersBottomSheet = false
//            },
//            sheetState = sheetState,
//        )
//    }

    BaseTriggerScreen(modifier, viewModel)
}
