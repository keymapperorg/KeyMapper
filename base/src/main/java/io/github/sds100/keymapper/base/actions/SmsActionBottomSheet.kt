package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsActionBottomSheetState(
    val actionId: ActionId,
    val number: String,
    val message: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.smsActionBottomSheetState != null) {
        SmsActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.smsActionBottomSheetState = null
            },
            state = delegate.smsActionBottomSheetState!!,
            onNumberChanged = {
                delegate.smsActionBottomSheetState =
                    delegate.smsActionBottomSheetState?.copy(number = it)
            },
            onMessageChanged = {
                delegate.smsActionBottomSheetState =
                    delegate.smsActionBottomSheetState?.copy(message = it)
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    val result = delegate.smsActionBottomSheetState ?: return@launch
                    delegate.smsActionBottomSheetState = null

                    val action = when (result.actionId) {
                        ActionId.SEND_SMS -> ActionData.SendSms(result.number, result.message)
                        ActionId.COMPOSE_SMS -> ActionData.ComposeSms(result.number, result.message)
                        else -> return@launch
                    }

                    delegate.actionResult.update { action }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: SmsActionBottomSheetState,
    onNumberChanged: (String) -> Unit = {},
    onMessageChanged: (String) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val numberEmptyErrorString = stringResource(R.string.hint_create_sms_action_number)
    val messageEmptyErrorString = stringResource(R.string.hint_create_sms_action_message)

    var numberError: String? by rememberSaveable { mutableStateOf(null) }
    var messageError: String? by rememberSaveable { mutableStateOf(null) }

    val title = when (state.actionId) {
        ActionId.SEND_SMS -> stringResource(R.string.action_send_sms)
        ActionId.COMPOSE_SMS -> stringResource(R.string.action_compose_sms)
        else -> ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = state.number,
                label = { Text(stringResource(R.string.hint_create_sms_action_number)) },
                onValueChange = {
                    numberError = null
                    onNumberChanged(it)
                },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                ),
                isError = numberError != null,
                supportingText = {
                    if (numberError != null) {
                        Text(
                            text = numberError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = state.message,
                label = { Text(stringResource(R.string.hint_create_sms_action_message)) },
                onValueChange = {
                    messageError = null
                    onMessageChanged(it)
                },
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                ),
                isError = messageError != null,
                supportingText = {
                    if (messageError != null) {
                        Text(
                            text = messageError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    var hasError = false

                    if (state.number.isBlank()) {
                        numberError = numberEmptyErrorString
                        hasError = true
                    }

                    if (state.message.isBlank()) {
                        messageError = messageEmptyErrorString
                        hasError = true
                    }

                    if (!hasError) {
                        onDoneClick()
                    }
                },
            ) {
                Text(stringResource(R.string.done))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                },
            ) {
                Text(stringResource(R.string.cancel))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
