package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.draggable.DraggableItem
import io.github.sds100.keymapper.compose.draggable.dragContainer
import io.github.sds100.keymapper.compose.draggable.rememberDragDropState
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.TextListItem
import io.github.sds100.keymapper.util.ui.compose.ListItemFixError
import io.github.sds100.keymapper.util.ui.compose.RadioButtonTextRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(modifier: Modifier = Modifier, viewModel: ConfigTriggerViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val setupGuiKeyboardState by viewModel.setupGuiKeyboardState.collectAsStateWithLifecycle()
    val recordTriggerState by viewModel.recordTriggerState.collectAsStateWithLifecycle()

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
                    onAdvancedTriggersClick = {
                        viewModel.showAdvancedTriggersBottomSheet = true
                    },
                    onSelectClickType = viewModel::onClickTypeRadioButtonChecked,
                    onSelectParallelMode = viewModel::onParallelRadioButtonChecked,
                    onSelectSequenceMode = viewModel::onSequenceRadioButtonChecked,
                    onMoveTriggerKey = viewModel::onMoveTriggerKey,
                    onFixErrorClick = viewModel::onTriggerErrorClick,
                    onClickShortcut = viewModel::onClickTriggerKeyShortcut,
                )
            } else {
                TriggerScreenVertical(
                    modifier = modifier,
                    configState = state.data,
                    recordTriggerState = recordTriggerState,
                    onRemoveClick = viewModel::onRemoveKeyClick,
                    onEditClick = viewModel::onTriggerKeyOptionsClick,
                    onRecordTriggerClick = viewModel::onRecordTriggerButtonClick,
                    onAdvancedTriggersClick = {
                        viewModel.showAdvancedTriggersBottomSheet = true
                    },
                    onSelectClickType = viewModel::onClickTypeRadioButtonChecked,
                    onSelectParallelMode = viewModel::onParallelRadioButtonChecked,
                    onSelectSequenceMode = viewModel::onSequenceRadioButtonChecked,
                    onMoveTriggerKey = viewModel::onMoveTriggerKey,
                    onFixErrorClick = viewModel::onTriggerErrorClick,
                    onClickShortcut = viewModel::onClickTriggerKeyShortcut,
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
    onFixErrorClick: (String) -> Unit = {},
    onClickShortcut: (TriggerKeyShortcut) -> Unit = {},
) {
    Surface(modifier = modifier) {
        Column {
            when (configState) {
                is ConfigTriggerState.Empty -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            modifier = Modifier.padding(32.dp),
                            text = stringResource(R.string.triggers_recyclerview_placeholder),
                            textAlign = TextAlign.Center,
                        )

                        TriggerKeyShortcutRowNoTrigger(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            shortcuts = configState.shortcuts,
                            onClick = onClickShortcut,
                        )
                    }
                }

                is ConfigTriggerState.Loaded -> {
                    Spacer(Modifier.height(8.dp))

                    ErrorList(errors = configState.errors, onFixErrorClick = onFixErrorClick)

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
                    )

                    if (configState.clickTypeButtons.isNotEmpty()) {
                        ClickTypeRadioGroup(
                            clickTypes = configState.clickTypeButtons,
                            checkedClickType = configState.checkedClickType,
                            onSelectClickType = onSelectClickType,
                        )
                    }

                    if (configState.triggerModeButtonsVisible) {
                        TriggerModeRadioGroup(
                            mode = configState.checkedTriggerMode,
                            isEnabled = configState.triggerModeButtonsEnabled,
                            onSelectParallelMode = onSelectParallelMode,
                            onSelectSequenceMode = onSelectSequenceMode,
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
    onFixErrorClick: (String) -> Unit = {},
    onClickShortcut: (TriggerKeyShortcut) -> Unit = {},
) {
    Surface(modifier = modifier) {
        when (configState) {
            is ConfigTriggerState.Empty -> Row {
                Text(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .padding(32.dp),
                    text = stringResource(R.string.triggers_recyclerview_placeholder),
                    textAlign = TextAlign.Center,
                )
                Column {
                    TriggerKeyShortcutRowNoTrigger(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .weight(1f),
                        shortcuts = configState.shortcuts,
                        onClick = onClickShortcut,
                    )
                    RecordTriggerButtonRow(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        onRecordTriggerClick = onRecordTriggerClick,
                        recordTriggerState = recordTriggerState,
                        onAdvancedTriggersClick = onAdvancedTriggersClick,
                    )
                }
            }

            is ConfigTriggerState.Loaded -> Row {
                TriggerList(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 400.dp)
                        .padding(vertical = 8.dp),
                    triggerList = configState.triggerKeys,
                    shortcuts = configState.shortcuts,
                    isReorderingEnabled = configState.isReorderingEnabled,
                    onEditClick = onEditClick,
                    onRemoveClick = onRemoveClick,
                    onMove = onMoveTriggerKey,
                    onClickShortcut = onClickShortcut,
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
                        ErrorList(
                            errors = configState.errors,
                            onFixErrorClick = onFixErrorClick,
                        )

                        if (configState.clickTypeButtons.isNotEmpty()) {
                            ClickTypeRadioGroup(
                                clickTypes = configState.clickTypeButtons,
                                checkedClickType = configState.checkedClickType,
                                onSelectClickType = onSelectClickType,
                            )
                        }

                        if (configState.triggerModeButtonsVisible) {
                            TriggerModeRadioGroup(
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
    shortcuts: Set<TriggerKeyShortcut>,
    isReorderingEnabled: Boolean,
    onRemoveClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onClickShortcut: (TriggerKeyShortcut) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        onMove = onMove,
        // Do not drag and drop the row of shortcuts
        ignoreLastItems = 1,
    )

    // Use dragContainer rather than .draggable() modifier because that causes
    // dragging the first item to be always be dropped in the next position.
    LazyColumn(
        modifier = modifier.let { modifier ->
            if (isReorderingEnabled) {
                modifier.dragContainer(dragDropState)
            } else {
                modifier
            }
        },
        state = lazyListState,
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
                    isDragging = isDragging,
                    isReorderingEnabled = isReorderingEnabled,
                    onRemoveClick = { onRemoveClick(model.id) },
                    onEditClick = { onEditClick(model.id) },
                )
            }
        }

        item(key = "shortcuts", contentType = "shortcuts") {
            TriggerKeyShortcutRow(
                modifier = Modifier.fillMaxWidth(),
                shortcuts = shortcuts,
                onClick = { onClickShortcut(it) },
            )
        }
    }
}

@Composable
private fun ErrorList(
    modifier: Modifier = Modifier,
    errors: List<TextListItem.Error>,
    onFixErrorClick: (String) -> Unit = {},
) {
    Column(modifier = modifier) {
        for (model in errors) {
            ListItemFixError(model = model, onFixClick = { onFixErrorClick(model.id) })
        }
    }
}

@Composable
private fun ClickTypeRadioGroup(
    modifier: Modifier = Modifier,
    clickTypes: Set<ClickType>,
    checkedClickType: ClickType?,
    onSelectClickType: (ClickType) -> Unit,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (clickTypes.contains(ClickType.SHORT_PRESS)) {
                RadioButtonTextRow(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.SHORT_PRESS,
                    text = stringResource(R.string.radio_button_short_press),
                    onSelected = { onSelectClickType(ClickType.SHORT_PRESS) },
                )
            }
            if (clickTypes.contains(ClickType.LONG_PRESS)) {
                RadioButtonTextRow(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.LONG_PRESS,
                    text = stringResource(R.string.radio_button_long_press),
                    onSelected = { onSelectClickType(ClickType.LONG_PRESS) },
                )
            }
            if (clickTypes.contains(ClickType.DOUBLE_PRESS)) {
                RadioButtonTextRow(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.DOUBLE_PRESS,
                    text = stringResource(R.string.radio_button_double_press),
                    onSelected = { onSelectClickType(ClickType.DOUBLE_PRESS) },
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
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(R.string.press_dot_dot_dot),
            style = MaterialTheme.typography.labelLarge,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            RadioButtonTextRow(
                modifier = Modifier.weight(1f),
                isSelected = mode is TriggerMode.Parallel,
                isEnabled = isEnabled,
                text = stringResource(R.string.radio_button_parallel),
                onSelected = onSelectParallelMode,
            )
            RadioButtonTextRow(
                modifier = Modifier.weight(1f),
                isSelected = mode == TriggerMode.Sequence,
                isEnabled = isEnabled,
                text = stringResource(R.string.radio_button_sequence),
                onSelected = onSelectSequenceMode,
            )
        }
    }
}

private val sampleList = listOf(
    TriggerKeyListItemModel(
        id = "vol_up",
        name = "Volume Up",
        clickTypeString = "Long Press",
        extraInfo = "External Keyboard",
        linkType = TriggerKeyLinkType.PLUS,
    ),
    TriggerKeyListItemModel(
        id = "vol_down",
        name = "Volume Down",
        clickTypeString = "Single Press",
        extraInfo = null,
        linkType = TriggerKeyLinkType.PLUS,
    ),
)

private val previewState = ConfigTriggerState.Loaded(
    triggerKeys = sampleList,
    errors = listOf(
        TextListItem.Error(
            id = "error",
            text = "DND Access denied",
        ),
    ),
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
    shortcuts = setOf(TriggerKeyShortcut.ASSISTANT),
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

@Preview(device = Devices.PIXEL)
@Composable
private fun VerticalEmptyPreview() {
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = ConfigTriggerState.Empty(
                shortcuts = setOf(
                    TriggerKeyShortcut.ASSISTANT,
                    TriggerKeyShortcut.FLOATING_BUTTON,
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
            configState = ConfigTriggerState.Empty(shortcuts = setOf(TriggerKeyShortcut.ASSISTANT)),
            recordTriggerState = RecordTriggerState.Idle,
        )
    }
}
