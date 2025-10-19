package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.models.isSuccess
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import kotlinx.coroutines.launch

data class ShellCommandActionState(
    val description: String = "",
    val command: String = "",
    val useRoot: Boolean = false,
    /**
     * UI works with seconds for user-friendliness
     */
    val timeoutSeconds: Int = 10,
    val isRunning: Boolean = false,
    val testResult: KMResult<ShellResult>? = null,
)

@Composable
fun ShellCommandActionScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigShellCommandViewModel
) {
    ShellCommandActionScreen(
        modifier = modifier,
        state = viewModel.state,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onCommandChanged = viewModel::onCommandChanged,
        onUseRootChanged = viewModel::onUseRootChanged,
        onTimeoutChanged = viewModel::onTimeoutChanged,
        onTestClick = viewModel::onTestClick,
        onKillClick = viewModel::onKillClick,
        onDoneClick = viewModel::onDoneClick,
        onCancelClick = viewModel::onCancelClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellCommandActionScreen(
    modifier: Modifier = Modifier,
    state: ShellCommandActionState,
    onDescriptionChanged: (String) -> Unit = {},
    onCommandChanged: (String) -> Unit = {},
    onUseRootChanged: (Boolean) -> Unit = {},
    onTimeoutChanged: (Int) -> Unit = {},
    onTestClick: () -> Unit = {},
    onKillClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var descriptionError: String? by rememberSaveable { mutableStateOf(null) }
    var commandError: String? by rememberSaveable { mutableStateOf(null) }
    val descriptionEmptyErrorString = stringResource(R.string.error_cant_be_empty)
    val commandEmptyErrorString = stringResource(R.string.action_shell_command_command_empty_error)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_shell_command_title)) },
            )
        },
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            var hasError = false

                            if (state.description.isBlank()) {
                                descriptionError = descriptionEmptyErrorString
                                hasError = true
                            }

                            if (state.command.isBlank()) {
                                commandError = commandEmptyErrorString
                                hasError = true
                            }

                            if (hasError) {
                                scope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                            } else {
                                onDoneClick()
                            }
                        },
                        text = { Text(stringResource(R.string.pos_done)) },
                        icon = {
                            Icon(Icons.Rounded.Check, stringResource(R.string.pos_done))
                        },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    )
                },
                actions = {
                    IconButton(onClick = onCancelClick) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.neg_cancel))
                    }
                },
            )
        },
    ) { innerPadding ->

        val layoutDirection = LocalLayoutDirection.current
        val startPadding = innerPadding.calculateStartPadding(layoutDirection)
        val endPadding = innerPadding.calculateEndPadding(layoutDirection)

        ShellCommandActionContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = startPadding,
                    end = endPadding,
                ),
            state = state,
            descriptionError = descriptionError,
            commandError = commandError,
            onDescriptionChanged = {
                descriptionError = null
                onDescriptionChanged(it)
            },
            onCommandChanged = {
                commandError = null
                onCommandChanged(it)
            },
            onUseRootChanged = onUseRootChanged,
            onTimeoutChanged = onTimeoutChanged,
            onTestClick = {
                if (state.command.isBlank()) {
                    commandError = commandEmptyErrorString
                } else {
                    onTestClick()
                }
            },
            onKillClick = onKillClick,
        )
    }
}

@Composable
private fun ShellCommandActionContent(
    modifier: Modifier = Modifier,
    state: ShellCommandActionState,
    descriptionError: String?,
    commandError: String?,
    onDescriptionChanged: (String) -> Unit,
    onCommandChanged: (String) -> Unit,
    onUseRootChanged: (Boolean) -> Unit,
    onTimeoutChanged: (Int) -> Unit,
    onTestClick: () -> Unit,
    onKillClick: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.description,
            onValueChange = onDescriptionChanged,
            label = { Text(stringResource(R.string.hint_shell_command_description)) },
            singleLine = true,
            isError = descriptionError != null,
            supportingText = {
                if (descriptionError != null) {
                    Text(
                        text = descriptionError,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.command,
            onValueChange = onCommandChanged,
            label = { Text(stringResource(R.string.action_shell_command_command_label)) },
            minLines = 3,
            maxLines = 10,
            isError = commandError != null,
            supportingText = {
                if (commandError != null) {
                    Text(
                        text = commandError,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
        )

        SliderOptionText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.hint_shell_command_timeout),
            defaultValue = 10f,
            value = state.timeoutSeconds.toFloat(),
            valueText = { "${it.toInt()}s" },
            onValueChange = { onTimeoutChanged(it.toInt()) },
            valueRange = 5f..60f,
            stepSize = 5,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckBoxText(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.action_shell_command_use_root_label),
                isChecked = state.useRoot,
                onCheckedChange = onUseRootChanged,
            )

            Button(
                onClick = onTestClick,
                enabled = !state.isRunning,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (state.isRunning) {
                        stringResource(R.string.action_shell_command_testing)
                    } else {
                        stringResource(R.string.action_shell_command_test_button)
                    }
                )
            }
        }

        if (state.isRunning) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onKillClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pos_kill))
            }
        }

        if (state.isRunning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.testResult != null) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        when (val result = state.testResult) {
            null -> {}
            is Success -> {
                val shellResult = result.value
                if (shellResult.isSuccess()) {
                    Text(
                        text = stringResource(R.string.action_shell_command_output_label),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    SelectionContainer {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = shellResult.stdOut,
                            onValueChange = {},
                            readOnly = true,
                            minLines = 5,
                            maxLines = 15,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.action_shell_command_test_failed),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    SelectionContainer {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = shellResult.stdErr,
                            onValueChange = {},
                            readOnly = true,
                            minLines = 5,
                            maxLines = 15,
                            isError = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                }

                Text(
                    text = stringResource(
                        R.string.action_shell_command_exit_code,
                        result.value.exitCode
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is KMError -> {
                Text(
                    text = stringResource(R.string.action_shell_command_test_failed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                Text(
                    text = result.getFullMessage(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Preview
@Composable
private fun PreviewShellCommandActionScreen() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            state = ShellCommandActionState(
                description = "Hello world script",
                command = "echo 'Hello World'",
                useRoot = false,
                testResult = Success(ShellResult("Hello World\nNew line\nNew new line", "", 0)),
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandActionScreenEmpty() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            state = ShellCommandActionState(
                description = "",
                command = "",
                useRoot = true,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandActionScreenError() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            state = ShellCommandActionState(
                description = "Read secret file",
                command = "cat /root/secret.txt",
                useRoot = true,
                testResult = SystemError.PermissionDenied(Permission.ROOT),
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandActionScreenShellError() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            state = ShellCommandActionState(
                description = "",
                command = "ls",
                useRoot = true,
                testResult = Success(
                    ShellResult(
                        stdOut = "",
                        stdErr = "ls: .: Permission denied",
                        exitCode = 1
                    )
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandActionScreenTesting() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            state = ShellCommandActionState(
                description = "Count to 10",
                command = "for i in \$(seq 1 10); do echo \"Line \$i\"; sleep 1; done",
                useRoot = false,
                isRunning = true,
                testResult = Success(ShellResult("Line 1\nLine 2\nLine 3\nLine 4\nLine 5", "", 0)),
            ),
        )
    }
}
