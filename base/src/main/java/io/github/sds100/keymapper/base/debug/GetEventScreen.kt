package io.github.sds100.keymapper.base.debug

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ExpertModeStatus

@Composable
fun GetEventScreen(
    modifier: Modifier = Modifier,
    viewModel: GetEventViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    GetEventScreen(
        modifier = modifier,
        state = viewModel.state,
        onBackClick = onBackClick,
        onToggleRecordClick = viewModel::onToggleRecordClick,
        onClearClick = viewModel::onClearClick,
        onCopyToClipboardClick = viewModel::onCopyToClipboardClick,
        onSaveToFileClick = viewModel::onSaveToFileClick,
        onSetupExpertModeClick = viewModel::onSetupExpertModeClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GetEventScreen(
    modifier: Modifier = Modifier,
    state: GetEventViewModel.State,
    onBackClick: () -> Unit = {},
    onToggleRecordClick: () -> Unit = {},
    onClearClick: () -> Unit = {},
    onCopyToClipboardClick: () -> Unit = {},
    onSaveToFileClick: () -> Unit = {},
    onSetupExpertModeClick: () -> Unit = {},
) {
    val hasOutput = state.output.isNotEmpty()

    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_debug_getevent)) },
            )
        },
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    if (state.expertModeStatus == ExpertModeStatus.ENABLED) {
                        val containerColor = if (state.isRecording) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                        val contentColor = if (state.isRecording) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        FloatingActionButton(
                            onClick = onToggleRecordClick,
                            containerColor = containerColor,
                            contentColor = contentColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            if (state.isRecording) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = stringResource(
                                        R.string.debug_getevent_stop_recording,
                                    ),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.FiberManualRecord,
                                    contentDescription = stringResource(
                                        R.string.debug_getevent_start_recording,
                                    ),
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_go_back),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onCopyToClipboardClick,
                        enabled = hasOutput,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.debug_getevent_copy),
                        )
                    }
                    IconButton(
                        onClick = onSaveToFileClick,
                        enabled = hasOutput,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.debug_getevent_save),
                        )
                    }
                    IconButton(
                        onClick = onClearClick,
                        enabled = hasOutput,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.debug_getevent_clear),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = innerPadding.calculateStartPadding(layoutDirection)
        val endPadding = innerPadding.calculateEndPadding(layoutDirection)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = startPadding,
                    end = endPadding,
                ),
        ) {
            Content(
                modifier = Modifier.fillMaxSize(),
                state = state,
                onSetupExpertModeClick = onSetupExpertModeClick,
            )
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: GetEventViewModel.State,
    onSetupExpertModeClick: () -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(state.output) {
        if (state.output.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = modifier) {
        if (state.expertModeStatus != ExpertModeStatus.ENABLED) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.debug_getevent_expert_mode_required),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = onSetupExpertModeClick,
                        colors = ButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_shell_command_setup_expert_mode))
                    }
                }
            }
        }

        if (state.isLoadingDeviceInfo) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        if (state.isRecording) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        if (state.output.isNotEmpty()) {
            SelectionContainer(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = state.output,
                    softWrap = false,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    lineHeight = 13.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun PreviewWithOutput() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventViewModel.State(
                output = """add device 1: /dev/input/event0
  bus:      0019
  vendor    0001
  product   0001
  version   0100
  name:     "gpio-keys"
  location: "gpio-keys/input0"
  id:       ""
  version:  1.0.1
  events:
    KEY (0001): KEY_POWER            
  input props:
    <none>
add device 2: /dev/input/event1
  bus:      0006
  vendor    0000
  product   0000
  version   0000
  name:     "virtio_input_multi_touch_1"
  location: "virtio10/input0"
  id:       ""
  version:  1.0.1
  events:
    KEY (0001): BTN_TOOL_RUBBER       BTN_STYLUS           
    ABS (0003): ABS_X                 : value 0, min 0, max 32767, fuzz 0, flat 0, resolution 0"""",
                expertModeStatus = ExpertModeStatus.ENABLED,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewRecording() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventViewModel.State(
                output = "add device 1: /dev/input/event0\n  name:     \"gpio-keys\"",
                isRecording = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewExpertModeDisabled() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventViewModel.State(
                expertModeStatus = ExpertModeStatus.DISABLED,
            ),
        )
    }
}
