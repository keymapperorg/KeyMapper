package io.github.sds100.keymapper.base.actions

import android.telephony.SmsManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import kotlinx.coroutines.launch

sealed class SmsActionBottomSheetState {
    abstract val number: String
    abstract val message: String

    data class SendSms(
        override val number: String,
        override val message: String,
        val testResult: State<KMResult<Unit>>?,
    ) : SmsActionBottomSheetState()

    data class ComposeSms(
        override val number: String,
        override val message: String,
    ) : SmsActionBottomSheetState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsActionBottomSheet(delegate: CreateActionDelegate) {
    rememberCoroutineScope()
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
                    when (val state = delegate.smsActionBottomSheetState) {
                        is SmsActionBottomSheetState.ComposeSms -> state.copy(number = it)
                        is SmsActionBottomSheetState.SendSms -> state.copy(number = it)
                        null -> null
                    }
            },
            onMessageChanged = {
                delegate.smsActionBottomSheetState =
                    when (val state = delegate.smsActionBottomSheetState) {
                        is SmsActionBottomSheetState.ComposeSms -> state.copy(message = it)
                        is SmsActionBottomSheetState.SendSms -> state.copy(message = it)
                        null -> null
                    }
            },
            onTestClick = delegate::onTestSmsClick,
            onDoneClick = delegate::onDoneSmsClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: SmsActionBottomSheetState,
    onTestClick: () -> Unit = {},
    onNumberChanged: (String) -> Unit = {},
    onMessageChanged: (String) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val numberEmptyErrorString = stringResource(R.string.error_cant_be_empty)
    val messageEmptyErrorString = stringResource(R.string.error_cant_be_empty)

    var numberError: String? by rememberSaveable { mutableStateOf(null) }
    var messageError: String? by rememberSaveable { mutableStateOf(null) }

    val title =
        when (state) {
            is SmsActionBottomSheetState.SendSms -> stringResource(R.string.action_send_sms)
            is SmsActionBottomSheetState.ComposeSms -> stringResource(R.string.action_compose_sms)
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier =
                    Modifier
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
                keyboardOptions =
                    KeyboardOptions(
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
                modifier =
                    Modifier
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
                keyboardOptions =
                    KeyboardOptions(
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                text = stringResource(R.string.warning_sms_charges),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state is SmsActionBottomSheetState.SendSms) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    when (state.testResult) {
                        is State.Data -> {
                            val result = state.testResult.data

                            val resultText: String =
                                when (result) {
                                    is Success -> stringResource(R.string.test_sms_result_ok)
                                    is KMError -> result.getFullMessage(LocalContext.current)
                                }

                            val textColor =
                                when (result) {
                                    is Success -> LocalCustomColorsPalette.current.green
                                    is KMError -> MaterialTheme.colorScheme.error
                                }

                            Text(
                                modifier = Modifier.weight(1f),
                                text = resultText,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        State.Loading -> {
                            CircularProgressIndicator()
                        }

                        null -> {}
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedButton(
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
                                onTestClick()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.button_test_sms))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.neg_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
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
                    Text(stringResource(R.string.pos_done))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState =
            SheetState(
                skipPartiallyExpanded = true,
                density = LocalDensity.current,
                initialValue = SheetValue.Expanded,
            )

        SmsActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state =
                SmsActionBottomSheetState.SendSms(
                    "+1 123456789",
                    "Message",
                    testResult = State.Loading,
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewTestError() {
    KeyMapperTheme {
        val sheetState =
            SheetState(
                skipPartiallyExpanded = true,
                density = LocalDensity.current,
                initialValue = SheetValue.Expanded,
            )

        SmsActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state =
                SmsActionBottomSheetState.SendSms(
                    "+1 123456789",
                    "Message",
                    testResult = State.Data(KMError.SendSmsError(SmsManager.RESULT_ERROR_NO_SERVICE)),
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewTestSuccess() {
    KeyMapperTheme {
        val sheetState =
            SheetState(
                skipPartiallyExpanded = true,
                density = LocalDensity.current,
                initialValue = SheetValue.Expanded,
            )

        SmsActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state =
                SmsActionBottomSheetState.SendSms(
                    "+1 123456789",
                    "Message",
                    testResult = State.Data(Success(Unit)),
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        val sheetState =
            SheetState(
                skipPartiallyExpanded = true,
                density = LocalDensity.current,
                initialValue = SheetValue.Expanded,
            )

        SmsActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state =
                SmsActionBottomSheetState.SendSms(
                    "",
                    "",
                    testResult = null,
                ),
        )
    }
}
