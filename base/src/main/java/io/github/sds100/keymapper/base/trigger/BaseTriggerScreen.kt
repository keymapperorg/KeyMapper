package io.github.sds100.keymapper.base.trigger

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.ShortcutButton
import io.github.sds100.keymapper.base.onboarding.TipCard
import io.github.sds100.keymapper.base.utils.ui.LinkType
import io.github.sds100.keymapper.base.utils.ui.compose.DraggableItem
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.base.utils.ui.compose.icons.ActionKey
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.rememberDragDropState
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseTriggerScreen(
    modifier: Modifier = Modifier,
    viewModel: BaseConfigTriggerViewModel,
    discoverScreenContent: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recordTriggerState by viewModel.recordTriggerState.collectAsStateWithLifecycle()
    val showFingerprintGestures: Boolean by viewModel.showFingerprintGesturesShortcut.collectAsStateWithLifecycle()

    HandleTriggerSetupBottomSheet(viewModel)

    if (viewModel.showDiscoverTriggersBottomSheet) {
        TriggerDiscoverBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                viewModel.showDiscoverTriggersBottomSheet = false
            },
            content = {
                TriggerDiscoverScreen(
                    showFloatingButtons = true,
                    showFingerprintGestures = showFingerprintGestures,
                    onShortcutClick = { shortcut ->
                        scope.launch {
                            sheetState.hide()
                            viewModel.showDiscoverTriggersBottomSheet = false
                            viewModel.showTriggerSetup(shortcut)
                        }
                    },
                )
            },
        )
    }

    val triggerKeyOptionsState by viewModel.triggerKeyOptionsState.collectAsStateWithLifecycle()

    if (triggerKeyOptionsState != null) {
        TriggerKeyOptionsBottomSheet(
            sheetState = sheetState,
            state = triggerKeyOptionsState!!,
            onDismissRequest = viewModel::onDismissTriggerKeyOptions,
            onCheckDoNotRemap = viewModel::onCheckDoNotRemap,
            onSelectClickType = viewModel::onSelectKeyClickType,
            onSelectDevice = viewModel::onSelectTriggerKeyDevice,
            onSelectAssistantType = viewModel::onSelectTriggerKeyAssistantType,
            onEditFloatingButtonClick = viewModel::onEditFloatingButtonClick,
            onEditFloatingLayoutClick = viewModel::onEditFloatingLayoutClick,
            onSelectFingerprintGestureType = viewModel::onSelectFingerprintGestureType,
            onScanCodeDetectionChanged = viewModel::onSelectScanCodeDetection,
        )
    }

    val configState by viewModel.state.collectAsStateWithLifecycle()
    val tipModel by viewModel.triggerTip.collectAsStateWithLifecycle()

    when (val state = configState) {
        is State.Loading -> Loading(modifier = modifier)
        is State.Data -> {
            val tipContent: @Composable () -> Unit = {
                tipModel?.let { tip ->
                    TipCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        title = tip.title,
                        message = tip.message,
                        isDismissable = tip.isDismissable,
                        onDismiss = viewModel::onTriggerTipDismissClick,
                        buttonText = tip.buttonText,
                        onButtonClick = { viewModel.onTipButtonClick(tip.id) },
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            if (isHorizontalLayout()) {
                TriggerScreenHorizontal(
                    modifier = modifier,
                    configState = state.data,
                    recordTriggerState = recordTriggerState,
                    onRemoveClick = viewModel::onRemoveKeyClick,
                    onEditClick = viewModel::onTriggerKeyOptionsClick,
                    onRecordTriggerClick = viewModel::onRecordTriggerButtonClick,
                    onAdvancedTriggersClick = viewModel::onAdvancedTriggersClick,
                    onSelectClickType = viewModel::onClickTypeRadioButtonChecked,
                    onSelectParallelMode = viewModel::onParallelRadioButtonChecked,
                    onSelectSequenceMode = viewModel::onSequenceRadioButtonChecked,
                    onMoveTriggerKey = viewModel::onMoveTriggerKey,
                    onFixErrorClick = viewModel::onTriggerErrorClick,
                    onAddMoreTriggerKeysClick = {
                        viewModel.showDiscoverTriggersBottomSheet = true
                    },
                    discoverScreenContent = discoverScreenContent,
                    tipContent = tipContent,
                )
            } else {
                TriggerScreenVertical(
                    modifier = modifier,
                    configState = state.data,
                    recordTriggerState = recordTriggerState,
                    onRemoveClick = viewModel::onRemoveKeyClick,
                    onEditClick = viewModel::onTriggerKeyOptionsClick,
                    onRecordTriggerClick = viewModel::onRecordTriggerButtonClick,
                    onAdvancedTriggersClick = viewModel::onAdvancedTriggersClick,
                    onSelectClickType = viewModel::onClickTypeRadioButtonChecked,
                    onSelectParallelMode = viewModel::onParallelRadioButtonChecked,
                    onSelectSequenceMode = viewModel::onSequenceRadioButtonChecked,
                    onMoveTriggerKey = viewModel::onMoveTriggerKey,
                    onFixErrorClick = viewModel::onTriggerErrorClick,
                    onAddMoreTriggerKeysClick = {
                        viewModel.showDiscoverTriggersBottomSheet = true
                    },
                    discoverScreenContent = discoverScreenContent,
                    tipContent = tipContent,
                )
            }
        }
    }
}

@Composable
private fun isHorizontalLayout(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    return windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT
}

@Composable
private fun isVerticalCompactLayout(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    return windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT &&
        windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TriggerScreenVertical(
    modifier: Modifier = Modifier,
    configState: ConfigTriggerState,
    recordTriggerState: RecordTriggerState,
    onRemoveClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onSelectClickType: (ClickType) -> Unit = {},
    onSelectParallelMode: () -> Unit = {},
    onSelectSequenceMode: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
    onAdvancedTriggersClick: () -> Unit = {},
    onMoveTriggerKey: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onFixErrorClick: (TriggerError) -> Unit = {},
    onAddMoreTriggerKeysClick: () -> Unit = {},
    discoverScreenContent: @Composable () -> Unit = {},
    tipContent: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier) {
        Column {
            val isCompact = isVerticalCompactLayout()

            when (configState) {
                is ConfigTriggerState.Empty -> {
                    Column {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(16.dp),
                        ) {
                            discoverScreenContent()
                        }

                        RecordTriggerButtonRow(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                            onRecordTriggerClick = onRecordTriggerClick,
                            recordTriggerState = recordTriggerState,
                            onAdvancedTriggersClick = onAdvancedTriggersClick,
                        )
                    }
                }

                is ConfigTriggerState.Loaded -> {
                    Spacer(Modifier.height(8.dp))

                    tipContent()

                    TriggerList(
                        modifier = Modifier.weight(1f),
                        triggerList = configState.triggerKeys,
                        isReorderingEnabled = configState.isReorderingEnabled,
                        onEditClick = onEditClick,
                        onRemoveClick = onRemoveClick,
                        onMove = onMoveTriggerKey,
                        onFixErrorClick = onFixErrorClick,
                        onAddMoreClick = onAddMoreTriggerKeysClick,
                    )

                    if (configState.clickTypeButtons.isNotEmpty()) {
                        ClickTypeSegmentedButtons(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            clickTypes = configState.clickTypeButtons,
                            checkedClickType = configState.checkedClickType,
                            onSelectClickType = onSelectClickType,
                            isCompact = isCompact,
                        )

                        if (!isCompact) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (configState.triggerModeButtonsVisible) {
                        TriggerModeSegmentedButtons(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            mode = configState.checkedTriggerMode,
                            isEnabled = configState.triggerModeButtonsEnabled,
                            onSelectParallelMode = onSelectParallelMode,
                            onSelectSequenceMode = onSelectSequenceMode,
                            isCompact = isCompact,
                        )
                    }
                }
            }

            if (!isCompact) {
                Spacer(Modifier.height(8.dp))
            }

            RecordTriggerButtonRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                onRecordTriggerClick = onRecordTriggerClick,
                recordTriggerState = recordTriggerState,
                onAdvancedTriggersClick = onAdvancedTriggersClick,
            )
        }
    }
}

@Composable
private fun TriggerScreenHorizontal(
    modifier: Modifier = Modifier,
    configState: ConfigTriggerState,
    recordTriggerState: RecordTriggerState,
    onRemoveClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onSelectClickType: (ClickType) -> Unit = {},
    onSelectParallelMode: () -> Unit = {},
    onSelectSequenceMode: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
    onAdvancedTriggersClick: () -> Unit = {},
    onMoveTriggerKey: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onFixErrorClick: (TriggerError) -> Unit = {},
    onAddMoreTriggerKeysClick: () -> Unit = {},
    discoverScreenContent: @Composable () -> Unit = {},
    tipContent: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier) {
        when (configState) {
            is ConfigTriggerState.Empty ->
                Row {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(16.dp),
                    ) {
                        discoverScreenContent()
                    }

                    RecordTriggerButtonRow(
                        modifier =
                            Modifier
                                .align(Alignment.Bottom)
                                .weight(1f)
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        onRecordTriggerClick = onRecordTriggerClick,
                        recordTriggerState = recordTriggerState,
                        onAdvancedTriggersClick = onAdvancedTriggersClick,
                    )
                }

            is ConfigTriggerState.Loaded ->
                Row {
                    TriggerList(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .widthIn(max = 400.dp),
                        triggerList = configState.triggerKeys,
                        isReorderingEnabled = configState.isReorderingEnabled,
                        onEditClick = onEditClick,
                        onRemoveClick = onRemoveClick,
                        onMove = onMoveTriggerKey,
                        onFixErrorClick = onFixErrorClick,
                        onAddMoreClick = onAddMoreTriggerKeysClick,
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            tipContent()

                            if (configState.clickTypeButtons.isNotEmpty()) {
                                ClickTypeSegmentedButtons(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                    clickTypes = configState.clickTypeButtons,
                                    checkedClickType = configState.checkedClickType,
                                    onSelectClickType = onSelectClickType,
                                    isCompact = false,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (configState.triggerModeButtonsVisible) {
                                TriggerModeSegmentedButtons(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                    mode = configState.checkedTriggerMode,
                                    isEnabled = configState.triggerModeButtonsEnabled,
                                    onSelectParallelMode = onSelectParallelMode,
                                    onSelectSequenceMode = onSelectSequenceMode,
                                    isCompact = false,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        RecordTriggerButtonRow(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                            onRecordTriggerClick = onRecordTriggerClick,
                            recordTriggerState = recordTriggerState,
                            onAdvancedTriggersClick = onAdvancedTriggersClick,
                        )
                    }
                }
        }
    }
}

@Composable
private fun TriggerList(
    modifier: Modifier = Modifier,
    triggerList: List<TriggerKeyListItemModel>,
    isReorderingEnabled: Boolean,
    onRemoveClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onFixErrorClick: (TriggerError) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddMoreClick: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(
            lazyListState = lazyListState,
            onMove = onMove,
            // Do not drag and drop the "add more" button
            ignoreLastItems =
                if (triggerList.isEmpty()) {
                    0
                } else {
                    1
                },
        )

    // Use dragContainer rather than .draggable() modifier because that causes
    // dragging the first item to be always be dropped in the next position.
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(
            triggerList,
            key = { _, item -> item.id },
            contentType = { _, _ -> "key" },
        ) { index, model ->
            DraggableItem(
                dragDropState = dragDropState,
                index = index,
            ) { isDragging ->
                TriggerKeyListItem(
                    modifier = Modifier.fillMaxWidth(),
                    model = model,
                    index = index,
                    isDraggingEnabled = triggerList.size > 1,
                    isDragging = isDragging,
                    isReorderingEnabled = isReorderingEnabled,
                    dragDropState = dragDropState,
                    onEditClick = { onEditClick(model.id) },
                    onRemoveClick = { onRemoveClick(model.id) },
                    onFixClick = onFixErrorClick,
                )
            }
        }

        if (triggerList.isNotEmpty()) {
            item(key = "add_more", contentType = "add_more") {
                ShortcutButton(
                    onClick = onAddMoreClick,
                    text = stringResource(R.string.trigger_list_add_more_button),
                    icon = {
                        Icon(KeyMapperIcons.ActionKey, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun ClickTypeSegmentedButtons(
    modifier: Modifier = Modifier,
    clickTypes: Set<ClickType>,
    checkedClickType: ClickType?,
    onSelectClickType: (ClickType) -> Unit,
    isCompact: Boolean,
) {
    // Always put the buttons in the same order
    val clickTypeButtonContent: List<Pair<ClickType, String>> =
        buildList {
            if (clickTypes.contains(ClickType.SHORT_PRESS)) {
                val text =
                    if (isCompact) {
                        stringResource(R.string.radio_button_short)
                    } else {
                        stringResource(R.string.radio_button_short_press)
                    }
                add(ClickType.SHORT_PRESS to text)
            }

            if (clickTypes.contains(ClickType.LONG_PRESS)) {
                val text =
                    if (isCompact) {
                        stringResource(R.string.radio_button_long)
                    } else {
                        stringResource(R.string.radio_button_long_press)
                    }
                add(ClickType.LONG_PRESS to text)
            }

            if (clickTypes.contains(ClickType.DOUBLE_PRESS)) {
                val text =
                    if (isCompact) {
                        stringResource(R.string.radio_button_double)
                    } else {
                        stringResource(R.string.radio_button_double_press)
                    }
                add(ClickType.DOUBLE_PRESS to text)
            }
        }

    KeyMapperSegmentedButtonRow(
        modifier = modifier,
        buttonStates = clickTypeButtonContent,
        selectedState = checkedClickType,
        onStateSelected = onSelectClickType,
        isCompact = isCompact,
    )
}

@Composable
private fun TriggerModeSegmentedButtons(
    modifier: Modifier = Modifier,
    mode: TriggerMode,
    isEnabled: Boolean,
    onSelectParallelMode: () -> Unit,
    onSelectSequenceMode: () -> Unit,
    isCompact: Boolean,
) {
    val triggerModeButtonContent =
        listOf(
            "parallel" to stringResource(R.string.radio_button_parallel),
            "sequence" to stringResource(R.string.radio_button_sequence),
        )

    KeyMapperSegmentedButtonRow(
        modifier = modifier,
        buttonStates = triggerModeButtonContent,
        selectedState =
            when (mode) {
                is TriggerMode.Parallel -> "parallel"
                TriggerMode.Sequence -> "sequence"
                TriggerMode.Undefined -> null
            },
        onStateSelected = { selectedMode ->
            when (selectedMode) {
                "parallel" -> onSelectParallelMode()
                "sequence" -> onSelectSequenceMode()
            }
        },
        isCompact = isCompact,
        isEnabled = isEnabled,
    )
}

private val sampleList =
    listOf(
        TriggerKeyListItemModel.KeyEvent(
            id = "id1",
            keyName = "Volume Up",
            clickType = ClickType.SHORT_PRESS,
            extraInfo = "External Keyboard",
            linkType = LinkType.ARROW,
            error = null,
        ),
        TriggerKeyListItemModel.FloatingButton(
            id = "id2",
            buttonName = "😎",
            layoutName = "Gaming",
            clickType = ClickType.DOUBLE_PRESS,
            linkType = LinkType.ARROW,
            error = null,
        ),
        TriggerKeyListItemModel.Assistant(
            id = "id3",
            assistantType = AssistantTriggerType.DEVICE,
            clickType = ClickType.DOUBLE_PRESS,
            linkType = LinkType.PLUS,
            error = null,
        ),
    )

private val previewState =
    ConfigTriggerState.Loaded(
        triggerKeys = sampleList,
        isReorderingEnabled = true,
        clickTypeButtons =
            setOf(
                ClickType.SHORT_PRESS,
                ClickType.LONG_PRESS,
                ClickType.DOUBLE_PRESS,
            ),
        checkedClickType = ClickType.LONG_PRESS,
        checkedTriggerMode = TriggerMode.Sequence,
        triggerModeButtonsEnabled = true,
        triggerModeButtonsVisible = true,
    )

@Preview(device = Devices.PIXEL)
@Composable
private fun VerticalPreview() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = previewState,
            recordTriggerState = RecordTriggerState.Idle,
            discoverScreenContent = {
                TriggerDiscoverScreen()
            },
        )
    }
}

@Preview(heightDp = 300, widthDp = 300)
@Composable
private fun VerticalPreviewTiny() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = previewState,
            recordTriggerState = RecordTriggerState.Idle,
            discoverScreenContent = {
                TriggerDiscoverScreen()
            },
        )
    }
}

@Preview(device = Devices.PIXEL)
@Composable
private fun VerticalEmptyPreview() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = ConfigTriggerState.Empty,
            recordTriggerState = RecordTriggerState.Idle,
            discoverScreenContent = {
                TriggerDiscoverScreen()
            },
        )
    }
}

@Preview(device = Devices.PIXEL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VerticalEmptyDarkPreview() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = ConfigTriggerState.Empty,
            recordTriggerState = RecordTriggerState.Idle,
            discoverScreenContent = {
                TriggerDiscoverScreen()
            },
        )
    }
}

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun HorizontalPreview() {
    KeyMapperTheme {
        TriggerScreenHorizontal(
            configState = previewState,
            recordTriggerState = RecordTriggerState.Idle,
            discoverScreenContent = {
                TriggerDiscoverScreen()
            },
            tipContent = {
                TipCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    title = "Tip Title",
                    message =
                        """
                        This is a tip message to help the user understand something about the current screen. 
                        It can be quite long so it should wrap properly.
                        """.trimIndent(),
                    onDismiss = {},
                )

                Spacer(Modifier.height(8.dp))
            },
        )
    }
}

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun HorizontalEmptyPreview() {
    KeyMapperTheme {
        TriggerScreenHorizontal(
            configState = ConfigTriggerState.Empty,
            recordTriggerState = RecordTriggerState.Idle,
            discoverScreenContent = {
                TriggerDiscoverScreen()
            },
        )
    }
}
