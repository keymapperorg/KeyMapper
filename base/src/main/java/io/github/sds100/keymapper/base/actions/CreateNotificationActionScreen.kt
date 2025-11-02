package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText

@Composable
fun CreateNotificationActionScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigCreateNotificationViewModel,
) {
    CreateNotificationActionScreen(
        modifier = modifier,
        state = viewModel.state,
        onTitleChanged = viewModel::onTitleChanged,
        onTextChanged = viewModel::onTextChanged,
        onTimeoutEnabledChanged = viewModel::onTimeoutEnabledChanged,
        onTimeoutChanged = viewModel::onTimeoutChanged,
        onDoneClick = viewModel::onDoneClick,
        onCancelClick = viewModel::onCancelClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateNotificationActionScreen(
    state: CreateNotificationActionState,
    onTitleChanged: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onTimeoutEnabledChanged: (Boolean) -> Unit,
    onTimeoutChanged: (Int) -> Unit,
    onDoneClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_create_notification)) },
                navigationIcon = {
                    IconButton(onClick = onCancelClick) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.pos_cancel),
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                ExtendedFloatingActionButton(
                    onClick = {
                        keyboardController?.hide()
                        onDoneClick()
                    },
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    text = { Text(stringResource(R.string.pos_done)) },
                    icon = {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.pos_done),
                        )
                    },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChanged,
                label = { Text(stringResource(R.string.action_create_notification_title_label)) },
                placeholder = { Text(stringResource(R.string.action_create_notification_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChanged,
                label = { Text(stringResource(R.string.action_create_notification_text_label)) },
                placeholder = { Text(stringResource(R.string.action_create_notification_text_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 10,
            )

            Spacer(modifier = Modifier.height(16.dp))

            CheckBoxText(
                label = stringResource(R.string.action_create_notification_timeout_checkbox),
                checked = state.timeoutEnabled,
                onCheckedChange = onTimeoutEnabledChanged,
            )

            if (state.timeoutEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                SliderOptionText(
                    label = stringResource(R.string.action_create_notification_timeout_label),
                    value = state.timeoutSeconds,
                    onValueChange = { onTimeoutChanged(it.toInt()) },
                    sliderValue = state.timeoutSeconds.toFloat(),
                    valueRange = 5f..300f,
                    steps = 58, // (300 - 5) / 5 - 1 = 58 steps for increments of 5 seconds
                    valueLabel = stringResource(
                        R.string.action_create_notification_timeout_value,
                        state.timeoutSeconds,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
private fun CreateNotificationActionScreenPreview() {
    KeyMapperTheme {
        CreateNotificationActionScreen(
            state = CreateNotificationActionState(
                title = "Test Notification",
                text = "This is a test notification message",
                timeoutEnabled = true,
                timeoutSeconds = 30,
            ),
            onTitleChanged = {},
            onTextChanged = {},
            onTimeoutEnabledChanged = {},
            onTimeoutChanged = {},
            onDoneClick = {},
            onCancelClick = {},
        )
    }
}
