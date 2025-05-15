package io.github.sds100.keymapper.trigger

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.keymaps.ClickType
import io.github.sds100.keymapper.keymaps.ShortcutModel
import io.github.sds100.keymapper.keymaps.ShortcutRow
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.LinkType
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.compose.DraggableItem
import io.github.sds100.keymapper.util.ui.compose.RadioButtonText
import io.github.sds100.keymapper.util.ui.compose.rememberDragDropState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(modifier: Modifier = Modifier, viewModel: ConfigTriggerViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val setupGuiKeyboardState by viewModel.setupGuiKeyboardState.collectAsStateWithLifecycle()
    val recordTriggerState by viewModel.recordTriggerState.collectAsStateWithLifecycle()

    HandleAssistantTriggerSetupBottomSheet(viewModel = viewModel)

    if (viewModel.showAdvancedTriggersBottomSheet) {
        AdvancedTriggersBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            viewModel = viewModel,
            onDismissRequest = {
                viewModel.showAdvancedTriggersBottomSheet = false
            },
            sheetState = sheetState,
        )
    }

    if (viewModel.showDpadTriggerSetupBottomSheet) {
        DpadTriggerSetupBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                viewModel.showDpadTriggerSetupBottomSheet = false
            },
            guiKeyboardState = setupGuiKeyboardState,
            onEnableKeyboardClick = viewModel::onEnableGuiKeyboardClick,
            onChooseKeyboardClick = viewModel::onChooseGuiKeyboardClick,
            onNeverShowAgainClick = viewModel::onNeverShowSetupDpadClick,
            sheetState = sheetState,
        )
    }

    if (viewModel.showNoKeysRecordedBottomSheet) {
        NoKeysRecordedBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                viewModel.showNoKeysRecordedBottomSheet = false
            },
            viewModel = viewModel,
            sheetState = sheetState,
        )
    }

    val triggerKeyOptionsState by viewModel.triggerKeyOptionsState.collectAsStateWithLifecycle()

    if (triggerKeyOptionsState != null) {
        TriggerKeyOptionsBottomSheet(
            modifier = Modifier.systemBarsPadding(),
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
        )
    }

    val configState by viewModel.state.collectAsStateWithLifecycle()

    when (val state = configState) {
        is State.Loading -> Loading(modifier = modifier)
        is State.Data -> {
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
                    onClickShortcut = viewModel::onClickTriggerKeyShortcut,
                    onRecordTriggerTapTargetCompleted = viewModel::onRecordTriggerTapTargetCompleted,
                    onSkipTapTarget = viewModel::onSkipTapTargetClick,
                    onAdvancedTriggerTapTargetCompleted = viewModel::onAdvancedTriggersTapTargetCompleted,
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
                    onClickShortcut = viewModel::onClickTriggerKeyShortcut,
                    onRecordTriggerTapTargetCompleted = viewModel::onRecordTriggerTapTargetCompleted,
                    onSkipTapTarget = viewModel::onSkipTapTargetClick,
                    onAdvancedTriggerTapTargetCompleted = viewModel::onAdvancedTriggersTapTargetCompleted,
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

    return windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT && windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
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
    onClickShortcut: (TriggerKeyShortcut) -> Unit = {},
    onRecordTriggerTapTargetCompleted: () -> Unit = {},
    onSkipTapTarget: () -> Unit = {},
    onAdvancedTriggerTapTargetCompleted: () -> Unit = {},
) {
    Surface(modifier = modifier) {
        Column {
            when (configState) {
                is ConfigTriggerState.Empty -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(state = rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            modifier = Modifier.padding(32.dp),
                            text = stringResource(R.string.triggers_recyclerview_placeholder),
                            textAlign = TextAlign.Center,
                        )

                        if (configState.shortcuts.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.trigger_shortcuts_header),
                                    style = MaterialTheme.typography.titleSmall,
                                )

                                Spacer(Modifier.height(8.dp))

                                ShortcutRow(
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp)
                                        .fillMaxWidth(),
                                    shortcuts = configState.shortcuts,
                                    onClick = onClickShortcut,
                                )
                            }
                        }
                    }
                }

                is ConfigTriggerState.Loaded -> {
                    val isCompact = isVerticalCompactLayout()
                    Spacer(Modifier.height(8.dp))

                    TriggerList(
                        modifier = Modifier.weight(1f),
                        triggerList = configState.triggerKeys,
                        shortcuts = configState.shortcuts,
                        isReorderingEnabled = configState.isReorderingEnabled,
                        onEditClick = onEditClick,
                        onRemoveClick = onRemoveClick,
                        onMove = onMoveTriggerKey,
                        onClickShortcut = onClickShortcut,
                        onFixErrorClick = onFixErrorClick,
                    )

                    if (configState.clickTypeButtons.isNotEmpty()) {
                        ClickTypeRadioGroup(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            clickTypes = configState.clickTypeButtons,
                            checkedClickType = configState.checkedClickType,
                            onSelectClickType = onSelectClickType,
                            maxLines = if (isCompact) 1 else 2,
                        )
                    }

                    if (configState.triggerModeButtonsVisible) {
                        if (!isCompact) {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                text = stringResource(R.string.press_dot_dot_dot),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        TriggerModeRadioGroup(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            mode = configState.checkedTriggerMode,
                            isEnabled = configState.triggerModeButtonsEnabled,
                            onSelectParallelMode = onSelectParallelMode,
                            onSelectSequenceMode = onSelectSequenceMode,
                            maxLines = if (isCompact) 1 else 2,
                        )
                    }
                }
            }

            RecordTriggerButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                onRecordTriggerClick = onRecordTriggerClick,
                recordTriggerState = recordTriggerState,
                onAdvancedTriggersClick = onAdvancedTriggersClick,
                showRecordTriggerTapTarget = (configState as? ConfigTriggerState.Empty)?.showRecordTriggerTapTarget
                    ?: false,
                onRecordTriggerTapTargetCompleted = onRecordTriggerTapTargetCompleted,
                onSkipTapTarget = onSkipTapTarget,
                showAdvancedTriggerTapTarget = configState.showAdvancedTriggersTapTarget,
                onAdvancedTriggerTapTargetCompleted = onAdvancedTriggerTapTargetCompleted,
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
    onClickShortcut: (TriggerKeyShortcut) -> Unit = {},
    onRecordTriggerTapTargetCompleted: () -> Unit = {},
    onSkipTapTarget: () -> Unit = {},
    onAdvancedTriggerTapTargetCompleted: () -> Unit = {},
) {
    Surface(modifier = modifier) {
        when (configState) {
            is ConfigTriggerState.Empty -> Row {
                Text(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .padding(32.dp)
                        .verticalScroll(state = rememberScrollState()),
                    text = stringResource(R.string.triggers_recyclerview_placeholder),
                    textAlign = TextAlign.Center,
                )
                Column {
                    if (configState.shortcuts.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(state = rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.trigger_shortcuts_header),
                                style = MaterialTheme.typography.titleSmall,
                            )

                            Spacer(Modifier.height(8.dp))

                            ShortcutRow(
                                modifier = Modifier
                                    .padding(horizontal = 32.dp)
                                    .fillMaxWidth(),
                                shortcuts = configState.shortcuts,
                                onClick = onClickShortcut,
                            )
                        }
                    }

                    RecordTriggerButtonRow(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        onRecordTriggerClick = onRecordTriggerClick,
                        recordTriggerState = recordTriggerState,
                        onAdvancedTriggersClick = onAdvancedTriggersClick,
                        showRecordTriggerTapTarget = (configState as? ConfigTriggerState.Empty)?.showRecordTriggerTapTarget
                            ?: false,
                        onRecordTriggerTapTargetCompleted = onRecordTriggerTapTargetCompleted,
                        onSkipTapTarget = onSkipTapTarget,
                        showAdvancedTriggerTapTarget = configState.showAdvancedTriggersTapTarget,
                    )
                }
            }

            is ConfigTriggerState.Loaded -> Row {
                TriggerList(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 400.dp),
                    triggerList = configState.triggerKeys,
                    shortcuts = configState.shortcuts,
                    isReorderingEnabled = configState.isReorderingEnabled,
                    onEditClick = onEditClick,
                    onRemoveClick = onRemoveClick,
                    onMove = onMoveTriggerKey,
                    onClickShortcut = onClickShortcut,
                    onFixErrorClick = onFixErrorClick,
                )

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (configState.clickTypeButtons.isNotEmpty()) {
                            ClickTypeRadioGroup(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                clickTypes = configState.clickTypeButtons,
                                checkedClickType = configState.checkedClickType,
                                onSelectClickType = onSelectClickType,
                            )
                        }

                        Text(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            text = stringResource(R.string.press_dot_dot_dot),
                            style = MaterialTheme.typography.labelLarge,
                        )

                        if (configState.triggerModeButtonsVisible) {
                            TriggerModeRadioGroup(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                mode = configState.checkedTriggerMode,
                                isEnabled = configState.triggerModeButtonsEnabled,
                                onSelectParallelMode = onSelectParallelMode,
                                onSelectSequenceMode = onSelectSequenceMode,
                            )
                        }
                    }

                    RecordTriggerButtonRow(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        onRecordTriggerClick = onRecordTriggerClick,
                        recordTriggerState = recordTriggerState,
                        onAdvancedTriggersClick = onAdvancedTriggersClick,
                        showRecordTriggerTapTarget = false,
                        onRecordTriggerTapTargetCompleted = onRecordTriggerTapTargetCompleted,
                        onSkipTapTarget = onSkipTapTarget,
                        showAdvancedTriggerTapTarget = configState.showAdvancedTriggersTapTarget,
                        onAdvancedTriggerTapTargetCompleted = onAdvancedTriggerTapTargetCompleted,
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
    shortcuts: Set<ShortcutModel<TriggerKeyShortcut>>,
    isReorderingEnabled: Boolean,
    onRemoveClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onFixErrorClick: (TriggerError) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onClickShortcut: (TriggerKeyShortcut) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        onMove = onMove,
        // Do not drag and drop the row of shortcuts
        ignoreLastItems = if (shortcuts.isEmpty()) {
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

        if (shortcuts.isNotEmpty()) {
            item(key = "shortcuts", contentType = "shortcuts") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.trigger_shortcuts_header),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    ShortcutRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        shortcuts = shortcuts,
                        onClick = { onClickShortcut(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClickTypeRadioGroup(
    modifier: Modifier = Modifier,
    clickTypes: Set<ClickType>,
    checkedClickType: ClickType?,
    onSelectClickType: (ClickType) -> Unit,
    maxLines: Int = 2,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (clickTypes.contains(ClickType.SHORT_PRESS)) {
                RadioButtonText(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.SHORT_PRESS,
                    text = stringResource(R.string.radio_button_short_press),
                    onSelected = { onSelectClickType(ClickType.SHORT_PRESS) },
                    maxLines = maxLines,
                )
            }
            if (clickTypes.contains(ClickType.LONG_PRESS)) {
                RadioButtonText(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.LONG_PRESS,
                    text = stringResource(R.string.radio_button_long_press),
                    onSelected = { onSelectClickType(ClickType.LONG_PRESS) },
                    maxLines = maxLines,
                )
            }
            if (clickTypes.contains(ClickType.DOUBLE_PRESS)) {
                RadioButtonText(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.DOUBLE_PRESS,
                    text = stringResource(R.string.radio_button_double_press),
                    onSelected = { onSelectClickType(ClickType.DOUBLE_PRESS) },
                    maxLines = maxLines,
                )
            }
        }
    }
}

@Composable
private fun TriggerModeRadioGroup(
    modifier: Modifier = Modifier,
    mode: TriggerMode,
    isEnabled: Boolean,
    onSelectParallelMode: () -> Unit,
    onSelectSequenceMode: () -> Unit,
    maxLines: Int = 2,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RadioButtonText(
                modifier = Modifier.weight(1f),
                isSelected = mode is TriggerMode.Parallel,
                isEnabled = isEnabled,
                text = stringResource(R.string.radio_button_parallel),
                onSelected = onSelectParallelMode,
                maxLines = maxLines,
            )
            RadioButtonText(
                modifier = Modifier.weight(1f),
                isSelected = mode == TriggerMode.Sequence,
                isEnabled = isEnabled,
                text = stringResource(R.string.radio_button_sequence),
                onSelected = onSelectSequenceMode,
                maxLines = maxLines,
            )
        }
    }
}

private val sampleList = listOf(
    TriggerKeyListItemModel.KeyCode(
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
        linkType = LinkType.HIDDEN,
        error = null,
    ),
)

private val previewState = ConfigTriggerState.Loaded(
    triggerKeys = sampleList,
    isReorderingEnabled = true,
    clickTypeButtons = setOf(
        ClickType.SHORT_PRESS,
        ClickType.LONG_PRESS,
        ClickType.DOUBLE_PRESS,
    ),
    checkedClickType = ClickType.LONG_PRESS,
    checkedTriggerMode = TriggerMode.Sequence,
    triggerModeButtonsEnabled = true,
    triggerModeButtonsVisible = true,
    shortcuts = setOf(
        ShortcutModel(
            icon = ComposeIconInfo.Vector(Icons.Rounded.Fingerprint),
            text = "Fingerprint gesture",
            data = TriggerKeyShortcut.FINGERPRINT_GESTURE,
        ),
    ),
)

@Preview(device = Devices.PIXEL)
@Composable
private fun VerticalPreview() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = previewState,
            recordTriggerState = RecordTriggerState.Idle,
        )
    }
}

@Preview(heightDp = 400, widthDp = 300)
@Composable
private fun VerticalPreviewTiny() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = previewState,
            recordTriggerState = RecordTriggerState.Idle,
        )
    }
}

@Preview(device = Devices.PIXEL)
@Composable
private fun VerticalEmptyPreview() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = ConfigTriggerState.Empty(
                shortcuts = setOf(
                    ShortcutModel(
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Fingerprint),
                        text = "Fingerprint gesture",
                        data = TriggerKeyShortcut.FINGERPRINT_GESTURE,
                    ),
                ),
            ),
            recordTriggerState = RecordTriggerState.Idle,
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
        )
    }
}

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun HorizontalEmptyPreview() {
    KeyMapperTheme {
        TriggerScreenHorizontal(
            configState = ConfigTriggerState.Empty(
                shortcuts = setOf(
                    ShortcutModel(
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Fingerprint),
                        text = "Fingerprint gesture",
                        data = TriggerKeyShortcut.FINGERPRINT_GESTURE,
                    ),
                ),

            ),
            recordTriggerState = RecordTriggerState.Idle,
        )
    }
}
