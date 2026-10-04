package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
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

    KMBottomSheet(
        title = stringResource(R.string.action_create_notification),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = {
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
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        if (!state.isPermissionGranted) {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestPermissionClick,
                colors = ButtonDefaults.filledTonalButtonColorsError(),
            ) {
                Text(stringResource(R.string.modify_setting_grant_permission_button))
            }

            Spacer(modifier = Modifier.height(16.dp))
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

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        CheckBoxText(
            text = stringResource(R.string.action_create_notification_timeout_checkbox),
            isChecked = state.timeoutEnabled,
            onCheckedChange = onTimeoutEnabledChange,
        )

        if (state.timeoutEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun CreateNotificationActionBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
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
@Preview(showSystemUi = true)
@Composable
private fun CreateNotificationActionBottomSheetEmptyPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        CreateNotificationActionBottomSheet(
            sheetState = sheetState,
            state = CreateNotificationActionBottomSheetState(
                isPermissionGranted = false,
            ),
        )
    }
}
