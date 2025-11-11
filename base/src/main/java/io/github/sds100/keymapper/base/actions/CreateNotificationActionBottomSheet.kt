package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import io.github.sds100.keymapper.base.utils.ui.compose.filledTonalButtonColorsError
import kotlinx.coroutines.launch

private const val MIN_TIMEOUT_SECONDS = 5
private const val MAX_TIMEOUT_SECONDS = 60
private const val TIMEOUT_STEP_SECONDS = 5

data class CreateNotificationActionBottomSheetState(
    val title: String = "",
    val text: String = "",
    val timeoutEnabled: Boolean = true,
    /**
     * UI works with seconds for user-friendliness
     */
    val timeoutSeconds: Int = 30,
    val isPermissionGranted: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotificationActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.createNotificationActionBottomSheetState != null) {
        CreateNotificationActionBottomSheet(
            sheetState = sheetState,
            state = delegate.createNotificationActionBottomSheetState!!,
            onRequestPermissionClick = delegate::onRequestNotificationPermissionClick,
            onDismissRequest = {
                delegate.createNotificationActionBottomSheetState = null
            },
            onTitleChange = delegate::onCreateNotificationTitleChange,
            onTextChange = delegate::onCreateNotificationTextChange,
            onTimeoutEnabledChange = delegate::onCreateNotificationTimeoutEnabledChange,
            onTimeoutChange = delegate::onCreateNotificationTimeoutChange,
            onTestClick = delegate::onTestCreateNotificationClick,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneCreateNotificationClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateNotificationActionBottomSheet(
    sheetState: SheetState,
    state: CreateNotificationActionBottomSheetState,
    onDismissRequest: () -> Unit = {},
    onRequestPermissionClick: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onTextChange: (String) -> Unit = {},
    onTimeoutEnabledChange: (Boolean) -> Unit = {},
    onTimeoutChange: (Int) -> Unit = {},
    onTestClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val titleEmptyErrorString = stringResource(R.string.action_create_notification_title_error)
    val textEmptyErrorString = stringResource(R.string.action_create_notification_text_error)

    var titleError: String? by rememberSaveable { mutableStateOf(null) }
    var textError: String? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect(state) {
        if (state.title.isNotBlank()) {
            titleError = null
        }

        if (state.text.isNotBlank()) {
            textError = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.action_create_notification),
                style = MaterialTheme.typography.headlineMedium,
            )

            if (!state.isPermissionGranted) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRequestPermissionClick,
                    colors = ButtonDefaults.filledTonalButtonColorsError(),
                ) {
                    Text(stringResource(R.string.modify_setting_grant_permission_button))
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.action_create_notification_title_label)) },
                placeholder = {
                    Text(stringResource(R.string.action_create_notification_title_hint))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(
                            text = titleError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.action_create_notification_text_label)) },
                placeholder = {
                    Text(stringResource(R.string.action_create_notification_text_hint))
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 10,
                isError = textError != null,
                supportingText = {
                    if (textError != null) {
                        Text(
                            text = textError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            CheckBoxText(
                text = stringResource(R.string.action_create_notification_timeout_checkbox),
                isChecked = state.timeoutEnabled,
                onCheckedChange = onTimeoutEnabledChange,
            )

            if (state.timeoutEnabled) {
                val timeoutValueFormat =
                    stringResource(R.string.action_create_notification_timeout_value)

                SliderOptionText(
                    title = stringResource(R.string.action_create_notification_timeout_label),
                    value = state.timeoutSeconds.toFloat(),
                    defaultValue = 30f,
                    valueText = { value ->
                        timeoutValueFormat.format(value.toInt())
                    },
                    onValueChange = { onTimeoutChange(it.toInt()) },
                    valueRange = MIN_TIMEOUT_SECONDS.toFloat()..MAX_TIMEOUT_SECONDS.toFloat(),
                    stepSize = TIMEOUT_STEP_SECONDS,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(
                    onClick = {
                        var hasError = false

                        if (state.title.isBlank()) {
                            titleError = titleEmptyErrorString
                            hasError = true
                        }

                        if (state.text.isBlank()) {
                            textError = textEmptyErrorString
                            hasError = true
                        }

                        if (!hasError) {
                            onTestClick()
                        }
                    },
                    enabled = state.isPermissionGranted,
                ) {
                    Text(stringResource(R.string.button_test_create_notification))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                        if (state.title.isBlank()) {
                            titleError = titleEmptyErrorString
                        }

                        if (state.text.isBlank()) {
                            textError = textEmptyErrorString
                        }

                        if (titleError == null && textError == null) {
                            onDoneClick()
                        }
                    },
                ) {
                    Text(stringResource(R.string.pos_done))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun CreateNotificationActionBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        CreateNotificationActionBottomSheet(
            sheetState = sheetState,
            state = CreateNotificationActionBottomSheetState(
                title = "Test Notification",
                text = "This is a test notification message",
                timeoutEnabled = true,
                timeoutSeconds = 30,
                isPermissionGranted = true,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun CreateNotificationActionBottomSheetEmptyPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        CreateNotificationActionBottomSheet(
            sheetState = sheetState,
            state = CreateNotificationActionBottomSheetState(
                isPermissionGranted = false,
            ),
        )
    }
}
