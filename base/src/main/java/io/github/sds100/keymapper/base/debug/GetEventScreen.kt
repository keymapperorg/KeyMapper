package io.github.sds100.keymapper.base.debug

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.launch

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
        onRefreshDeviceInfoClick = viewModel::onRefreshDeviceInfoClick,
        onCopyToClipboardClick = viewModel::onCopyToClipboardClick,
        onSaveToFileClick = viewModel::onSaveToFileClick,
        onSetupExpertModeClick = viewModel::onSetupExpertModeClick,
    )
}

private enum class GetEventTab {
    INFO,
    EVENTS,
}

private enum class RefreshButtonState {
    REFRESH_INFO,
    START_RECORDING,
    STOP,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GetEventScreen(
    modifier: Modifier = Modifier,
    state: GetEventState,
    onBackClick: () -> Unit = {},
    onToggleRecordClick: () -> Unit = {},
    onRefreshDeviceInfoClick: () -> Unit = {},
    onCopyToClipboardClick: () -> Unit = {},
    onSaveToFileClick: () -> Unit = {},
    onSetupExpertModeClick: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val isExpertModeEnabled = state.expertModeStatus == ExpertModeStatus.ENABLED
    val selectedTab = if (pagerState.currentPage == 0) GetEventTab.INFO else GetEventTab.EVENTS
    val hasOutputForSelectedTab =
        state.deviceInfoOutput.isNotEmpty() || state.recordingOutput.isNotEmpty()
    val refreshButtonState = when {
        selectedTab == GetEventTab.INFO -> RefreshButtonState.REFRESH_INFO
        state.isRecording -> RefreshButtonState.STOP
        else -> RefreshButtonState.START_RECORDING
    }

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
                    val containerColor = if (refreshButtonState == RefreshButtonState.STOP) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                    val contentColor = if (refreshButtonState == RefreshButtonState.STOP) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    FloatingActionButton(
                        modifier = if (isExpertModeEnabled) Modifier else Modifier.alpha(0.5f),
                        onClick = {
                            if (!isExpertModeEnabled) {
                                return@FloatingActionButton
                            }
                            if (selectedTab == GetEventTab.INFO) {
                                onRefreshDeviceInfoClick()
                            } else {
                                if (!state.isRecording) {
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                }
                                onToggleRecordClick()
                            }
                        },
                        containerColor = containerColor,
                        contentColor = contentColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    ) {
                        AnimatedContent(
                            targetState = refreshButtonState,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                            },
                            label = "refresh_button_state",
                        ) { buttonState ->
                            when (buttonState) {
                                RefreshButtonState.REFRESH_INFO -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = stringResource(
                                            R.string.debug_getevent_refresh,
                                        ),
                                    )
                                }

                                RefreshButtonState.START_RECORDING -> {
                                    Icon(
                                        imageVector = Icons.Rounded.FiberManualRecord,
                                        contentDescription = stringResource(
                                            R.string.debug_getevent_start_recording,
                                        ),
                                    )
                                }

                                RefreshButtonState.STOP -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = stringResource(
                                            R.string.debug_getevent_stop_recording,
                                        ),
                                    )
                                }
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
                        enabled = hasOutputForSelectedTab,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.debug_getevent_copy),
                        )
                    }
                    IconButton(
                        onClick = onSaveToFileClick,
                        enabled = hasOutputForSelectedTab,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.debug_getevent_save),
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
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.expertModeStatus != ExpertModeStatus.ENABLED) {
                    ExpertModeSetupCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onSetupExpertModeClick = onSetupExpertModeClick,
                    )
                }
                PrimaryTabRow(
                    selectedTabIndex = pagerState.targetPage,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Tab(
                        selected = pagerState.targetPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text(stringResource(R.string.debug_getevent_tab_info)) },
                    )
                    Tab(
                        selected = pagerState.targetPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text(stringResource(R.string.debug_getevent_tab_events)) },
                    )
                }

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> InfoContent(
                            modifier = Modifier.fillMaxSize(),
                            state = state,
                        )

                        1 -> EventsContent(
                            modifier = Modifier.fillMaxSize(),
                            state = state,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpertModeSetupCard(
    modifier: Modifier = Modifier,
    onSetupExpertModeClick: () -> Unit,
) {
    ElevatedCard(modifier = modifier) {
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

@Composable
private fun InfoContent(modifier: Modifier = Modifier, state: GetEventState) {
    Column(modifier = modifier) {
        if (state.isLoadingDeviceInfo) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (state.deviceInfoOutput.isNotEmpty()) {
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = state.deviceInfoOutput,
                    softWrap = false,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    lineHeight = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun EventsContent(modifier: Modifier = Modifier, state: GetEventState) {
    val verticalScrollState = rememberScrollState()

    LaunchedEffect(state.recordingOutput) {
        if (state.recordingOutput.isNotEmpty()) {
            verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
        }
    }

    Column(modifier = modifier) {
        if (state.isRecording) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(R.string.debug_getevent_events_output_after_recording),
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (state.recordingOutput.isEmpty()) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(R.string.debug_getevent_events_empty),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(verticalScrollState),
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = state.recordingOutput,
                    softWrap = false,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    lineHeight = 13.sp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewInfoTab() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventState(
                deviceInfoOutput = """add device 1: /dev/input/event0
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
    ABS (0003): ABS_X                 : value 0, min 0, max 32767, fuzz 0, flat 0, resolution 0""",
                expertModeStatus = ExpertModeStatus.ENABLED,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewInfoTabLoading() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventState(
                isLoadingDeviceInfo = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewInfoTabEmptyOutput() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventState(
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
            state = GetEventState(
                recordingOutput = """/dev/input/event1: EV_KEY       KEY_VOLUMEDOWN       DOWN
/dev/input/event1: EV_SYN       SYN_REPORT           00""",
                isRecording = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewEventsContentOutputIdle() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventState(
                recordingOutput = """/dev/input/event2: EV_KEY       KEY_VOLUMEUP         DOWN
/dev/input/event2: EV_SYN       SYN_REPORT           00""",
                isRecording = false,
                expertModeStatus = ExpertModeStatus.ENABLED,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewInfoContentOutputAndLoading() {
    KeyMapperTheme {
        GetEventScreen(
            state = GetEventState(
                deviceInfoOutput = """add device 3: /dev/input/event2
  bus:      0019
  vendor    0001
  product   0001
  version   0100
  name:     "gpio-keys-2"
  location: "gpio-keys/input1\"""",
                isLoadingDeviceInfo = true,
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
            state = GetEventState(
                expertModeStatus = ExpertModeStatus.DISABLED,
            ),
        )
    }
}
