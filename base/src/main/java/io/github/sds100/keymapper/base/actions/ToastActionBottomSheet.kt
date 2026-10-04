package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import kotlinx.coroutines.launch

data class ToastActionBottomSheetState(
    val message: String = "",
    val duration: ActionData.Toast.Duration = ActionData.Toast.Duration.SHORT,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToastActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.toastActionBottomSheetState != null) {
        ToastActionBottomSheet(
            sheetState = sheetState,
            state = delegate.toastActionBottomSheetState!!,
            onDismissRequest = { delegate.toastActionBottomSheetState = null },
            onMessageChange = delegate::onToastMessageChange,
            onDurationChange = delegate::onToastDurationChange,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneToastClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToastActionBottomSheet(
    sheetState: SheetState,
    state: ToastActionBottomSheetState,
    onDismissRequest: () -> Unit = {},
    onMessageChange: (String) -> Unit = {},
    onDurationChange: (ActionData.Toast.Duration) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val messageEmptyError = stringResource(R.string.action_toast_message_error)
    var messageError: String? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect(state.message) {
        if (state.message.isNotBlank()) {
            messageError = null
        }
    }

    KMBottomSheet(
        title = stringResource(R.string.action_toast),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = {
            if (state.message.isBlank()) {
                messageError = messageEmptyError
            } else {
                onDoneClick()
            }
        },
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.message,
            onValueChange = onMessageChange,
            label = { Text(stringResource(R.string.action_toast_message_label)) },
            placeholder = { Text(stringResource(R.string.action_toast_message_hint)) },
            singleLine = true,
            isError = messageError != null,
            supportingText = {
                if (messageError != null) {
                    Text(text = messageError!!, color = MaterialTheme.colorScheme.error)
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        KeyMapperSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
            buttonStates = listOf(
                ActionData.Toast.Duration.SHORT to
                    stringResource(R.string.action_toast_duration_short),
                ActionData.Toast.Duration.LONG to
                    stringResource(R.string.action_toast_duration_long),
            ),
            selectedState = state.duration,
            onStateSelected = onDurationChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun ToastActionBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        ToastActionBottomSheet(
            sheetState = sheetState,
            state = ToastActionBottomSheetState(
                message = "Hello world",
                duration = ActionData.Toast.Duration.SHORT,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun ToastActionBottomSheetEmptyPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        ToastActionBottomSheet(
            sheetState = sheetState,
            state = ToastActionBottomSheetState(),
        )
    }
}
