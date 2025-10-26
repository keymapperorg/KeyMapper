package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ProModeStatus
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.models.isError
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
    val executionMode: ShellExecutionMode = ShellExecutionMode.STANDARD,
    /**
     * UI works with seconds for user-friendliness
     */
    val timeoutSeconds: Int = 10,
    val isRunning: Boolean = false,
    val testResult: KMResult<ShellResult>? = null,
    val proModeStatus: ProModeStatus = ProModeStatus.UNSUPPORTED,
)

@Composable
fun ShellCommandActionScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigShellCommandViewModel,
) {
    ShellCommandActionScreen(
        modifier = modifier,
        state = viewModel.state,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onCommandChanged = viewModel::onCommandChanged,
        onExecutionModeChanged = viewModel::onExecutionModeChanged,
        onTimeoutChanged = viewModel::onTimeoutChanged,
        onTestClick = viewModel::onTestClick,
        onKillClick = viewModel::onKillClick,
        onDoneClick = viewModel::onDoneClick,
        onCancelClick = viewModel::onCancelClick,
        onSetupProModeClick = viewModel::onSetupProModeClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellCommandActionScreen(
    modifier: Modifier = Modifier,
    state: ShellCommandActionState,
    onDescriptionChanged: (String) -> Unit = {},
    onCommandChanged: (String) -> Unit = {},
    onExecutionModeChanged: (ShellExecutionMode) -> Unit = {},
    onTimeoutChanged: (Int) -> Unit = {},
    onTestClick: () -> Unit = {},
    onKillClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onSetupProModeClick: () -> Unit = {},
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = startPadding,
                    end = endPadding,
                ),
        ) {
            val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0)

            PrimaryTabRow(
                selectedTabIndex = pagerState.targetPage,
                modifier = Modifier.fillMaxWidth(),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Tab(
                    selected = pagerState.targetPage == 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = { Text(stringResource(R.string.action_shell_command_tab_configuration)) },
                )
                Tab(
                    selected = pagerState.targetPage == 1,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = { Text(stringResource(R.string.action_shell_command_tab_output)) },
                )
            }

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                contentPadding = PaddingValues(16.dp),
                pageSpacing = 16.dp,
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> ShellCommandConfigurationContent(
                        modifier = Modifier.fillMaxSize(),
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
                        onExecutionModeChanged = onExecutionModeChanged,
                        onTimeoutChanged = onTimeoutChanged,
                        onTestClick = {
                            if (state.command.isBlank()) {
                                commandError = commandEmptyErrorString
                            } else {
                                onTestClick()
                                scope.launch {
                                    pagerState.animateScrollToPage(1) // Switch to output tab
                                }
                            }
                        },
                        onSetupProModeClick = onSetupProModeClick,
                    )

                    1 -> ShellCommandOutputContent(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        onKillClick = onKillClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShellCommandConfigurationContent(
    modifier: Modifier = Modifier,
    state: ShellCommandActionState,
    descriptionError: String?,
    commandError: String?,
    onDescriptionChanged: (String) -> Unit,
    onCommandChanged: (String) -> Unit,
    onExecutionModeChanged: (ShellExecutionMode) -> Unit,
    onTimeoutChanged: (Int) -> Unit,
    onTestClick: () -> Unit,
    onSetupProModeClick: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
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

        Text(
            text = stringResource(R.string.action_shell_command_execution_mode_label),
            style = MaterialTheme.typography.titleMedium,
        )

        KeyMapperSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
            buttonStates = listOf(
                ShellExecutionMode.STANDARD to stringResource(R.string.action_shell_command_execution_mode_standard),
                ShellExecutionMode.ROOT to stringResource(R.string.action_shell_command_execution_mode_root),
                ShellExecutionMode.ADB to stringResource(R.string.action_shell_command_execution_mode_adb),
            ),
            selectedState = state.executionMode,
            onStateSelected = onExecutionModeChanged,
        )

        if (state.executionMode == ShellExecutionMode.ADB && state.proModeStatus != ProModeStatus.ENABLED) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSetupProModeClick,
                enabled = state.proModeStatus != ProModeStatus.UNSUPPORTED,
            ) {
                Text(
                    if (state.proModeStatus == ProModeStatus.UNSUPPORTED) {
                        stringResource(R.string.action_shell_command_setup_pro_mode_unsupported)
                    } else {
                        stringResource(R.string.action_shell_command_setup_pro_mode)
                    },
                )
            }
        }

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                keyboardController?.hide()
                onTestClick()
            },
            enabled = !state.isRunning &&
                (
                    state.executionMode != ShellExecutionMode.ADB ||
                        (
                            state.executionMode == ShellExecutionMode.ADB &&
                                state.proModeStatus == ProModeStatus.ENABLED
                            )
                    ),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (state.isRunning) {
                    stringResource(R.string.action_shell_command_testing)
                } else {
                    stringResource(R.string.action_shell_command_test_button)
                },
            )
        }
    }
}

@Composable
private fun ShellCommandOutputContent(
    modifier: Modifier = Modifier,
    state: ShellCommandActionState,
    onKillClick: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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

            if (state.executionMode == ShellExecutionMode.ADB) {
                Text(
                    text = stringResource(R.string.action_shell_command_adb_streaming_warning),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (val result = state.testResult) {
            null -> {
                if (!state.isRunning) {
                    Text(
                        text = stringResource(R.string.action_shell_command_no_output),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is Success -> {
                val shellResult = result.value

                if (shellResult.isSuccess()) {
                    Text(
                        text = stringResource(R.string.action_shell_command_output_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else if (shellResult.isError()) {
                    Text(
                        text = stringResource(R.string.action_shell_command_test_failed),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutputTextField(text = shellResult.stdout, isError = shellResult.isError())

                val exitCode = result.value.exitCode

                if (exitCode != null) {
                    Text(
                        text = stringResource(
                            R.string.action_shell_command_exit_code,
                            exitCode,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

                if (result is KMError.ShellCommandTimeout && result.stdout != null) {
                    OutputTextField(text = result.stdout!!, isError = true)
                }
            }
        }
    }
}

@Composable
private fun OutputTextField(modifier: Modifier = Modifier, text: String, isError: Boolean) {
    SelectionContainer(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = {},
            readOnly = true,
            minLines = 5,
            maxLines = 15,
            isError = isError,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandActionScreen() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            state = ShellCommandActionState(
                description = "Hello world script",
                command = "echo 'Hello World'",
                executionMode = ShellExecutionMode.STANDARD,
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
                executionMode = ShellExecutionMode.ROOT,
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
                executionMode = ShellExecutionMode.ROOT,
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
                executionMode = ShellExecutionMode.ROOT,
                testResult = Success(
                    ShellResult(
                        stdout = "ls: .: Permission denied",
                        exitCode = 1,
                    ),
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
                executionMode = ShellExecutionMode.STANDARD,
                isRunning = true,
                testResult = Success(ShellResult("Line 1\nLine 2\nLine 3\nLine 4\nLine 5", 0)),
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandActionScreenProModeUnsupported() {
    KeyMapperTheme {
        ShellCommandActionScreen(
            state = ShellCommandActionState(
                description = "ADB command example",
                command = "echo 'Hello from ADB'",
                executionMode = ShellExecutionMode.ADB,
                proModeStatus = ProModeStatus.UNSUPPORTED,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewShellCommandOutputSuccess() {
    KeyMapperTheme {
        Surface {
            ShellCommandOutputContent(
                state = ShellCommandActionState(
                    description = "Hello world script",
                    command = "echo 'Hello World'",
                    executionMode = ShellExecutionMode.STANDARD,
                    testResult = Success(ShellResult("Hello World\nNew line\nNew new line", 0)),
                ),
                onKillClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewShellCommandOutputError() {
    KeyMapperTheme {
        Surface {
            ShellCommandOutputContent(
                state = ShellCommandActionState(
                    description = "Read secret file",
                    command = "cat /root/secret.txt",
                    executionMode = ShellExecutionMode.ROOT,
                    testResult = SystemError.PermissionDenied(Permission.ROOT),
                ),
                onKillClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewShellCommandOutpuTimeout() {
    KeyMapperTheme {
        Surface {
            ShellCommandOutputContent(
                state = ShellCommandActionState(
                    description = "Read secret file",
                    command = "cat /root/secret.txt",
                    executionMode = ShellExecutionMode.ROOT,
                    testResult = KMError.ShellCommandTimeout(1000L, "1\n2\n3"),
                ),
                onKillClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewShellCommandOutputShellError() {
    KeyMapperTheme {
        Surface {
            ShellCommandOutputContent(
                state = ShellCommandActionState(
                    description = "List files",
                    command = "ls",
                    executionMode = ShellExecutionMode.ROOT,
                    testResult = Success(
                        ShellResult(
                            stdout = "ls: .: Permission denied",
                            exitCode = 1,
                        ),
                    ),
                ),
                onKillClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewShellCommandOutputRunning() {
    KeyMapperTheme {
        Surface {
            ShellCommandOutputContent(
                state = ShellCommandActionState(
                    description = "Count to 10",
                    command = "for i in $(seq 1 10); do echo \"Line \$i\"; sleep 1; done",
                    executionMode = ShellExecutionMode.STANDARD,
                    isRunning = true,
                    testResult = Success(
                        ShellResult(
                            "Line 1\nLine 2\nLine 3\nLine 4\nLine 5",
                            0,
                        ),
                    ),
                ),
                onKillClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewShellCommandOutputEmpty() {
    KeyMapperTheme {
        Surface {
            ShellCommandOutputContent(
                state = ShellCommandActionState(
                    description = "No output yet",
                    command = "echo 'Hello World'",
                    executionMode = ShellExecutionMode.STANDARD,
                ),
                onKillClick = {},
            )
        }
    }
}
