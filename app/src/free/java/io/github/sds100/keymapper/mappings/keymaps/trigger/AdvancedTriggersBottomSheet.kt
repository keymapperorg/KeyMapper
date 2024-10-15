package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.sds100.keymapper.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTriggersBottomSheet(
    modifier: Modifier = Modifier,
    viewModel: ConfigTriggerViewModel,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Text("I am free build.")
        IconButton(onClick = {
            scope.launch {
                sheetState.hide()
                onDismissRequest()
            }
        }) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.button_dismiss_advanced_triggers_sheet_content_description),
            )
        }
    }
}
