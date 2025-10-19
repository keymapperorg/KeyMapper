package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellCommandActionScreen(
    command: String,
    useRoot: Boolean,
    onCommandChanged: (String) -> Unit,
    onUseRootChanged: (Boolean) -> Unit,
    onTestClick: () -> Unit,
    onDoneClick: () -> Unit,
    onCancelClick: () -> Unit,
    testResult: State<String>? = null,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var commandError: String? by rememberSaveable { mutableStateOf(null) }
    val commandEmptyErrorString = stringResource(R.string.action_shell_command_command_empty_error)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_shell_command_title)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = command,
                onValueChange = {
                    commandError = null
                    onCommandChanged(it)
                },
                label = { Text(stringResource(R.string.action_shell_command_command_label)) },
                minLines = 3,
                maxLines = 10,
                isError = commandError != null,
                supportingText = {
                    if (commandError != null) {
                        Text(
                            text = commandError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = useRoot,
                    onCheckedChange = onUseRootChanged,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_shell_command_use_root_label))
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (command.isBlank()) {
                        commandError = commandEmptyErrorString
                    } else {
                        onTestClick()
                    }
                },
            ) {
                Text(stringResource(R.string.action_shell_command_test_button))
            }

            if (testResult != null) {
                Text(
                    text = stringResource(R.string.action_shell_command_output_label),
                    style = MaterialTheme.typography.titleMedium,
                )

                when (testResult) {
                    is State.Loading -> {
                        Text(
                            text = stringResource(R.string.action_shell_command_testing),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is State.Data -> {
                        SelectionContainer {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = testResult.data,
                                onValueChange = {},
                                readOnly = true,
                                minLines = 5,
                                maxLines = 15,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCancelClick,
                ) {
                    Text(stringResource(R.string.neg_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (command.isBlank()) {
                            commandError = commandEmptyErrorString
                        } else {
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

@Preview
@Composable
private fun PreviewShellCommandActionScreen() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            command = "echo 'Hello World'",
            useRoot = false,
            onCommandChanged = {},
            onUseRootChanged = {},
            onTestClick = {},
            onDoneClick = {},
            onCancelClick = {},
            testResult = State.Data("Hello World\n"),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandActionScreenEmpty() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            command = "",
            useRoot = true,
            onCommandChanged = {},
            onUseRootChanged = {},
            onTestClick = {},
            onDoneClick = {},
            onCancelClick = {},
        )
    }
}
