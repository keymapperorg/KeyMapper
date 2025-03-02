package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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

    val configState by viewModel.state.collectAsStateWithLifecycle()

    when (val state = configState) {
        is State.Loading -> Loading(modifier = modifier)
        is State.Data -> {
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
            )
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

// TODO handle horizontal layout
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
) {
    Surface {
        Column(modifier = modifier) {
            Spacer(Modifier.height(8.dp))

            TriggerList(
                modifier = Modifier.weight(1f),
                triggerList = configState.triggerKeys,
                isReorderingEnabled = configState.isReorderingEnabled,
                onEditClick = onEditClick,
                onRemoveClick = onRemoveClick,
                onMove = onMoveTriggerKey,
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
private fun TriggerList(
    modifier: Modifier = Modifier,
    triggerList: List<TriggerKeyListItemModel>,
    isReorderingEnabled: Boolean,
    onRemoveClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        onMove = onMove,
    )

    LazyColumn(
        // Use dragContainer rather than .draggable() modifier because that causes
        // dragging the first item to be always be dropped in the next position.
        modifier = modifier.dragContainer(dragDropState),
        state = lazyListState,
    ) {
        itemsIndexed(triggerList, key = { _, item -> item.id }) { index, model ->
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
                RadioButtonText(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.SHORT_PRESS,
                    text = stringResource(R.string.radio_button_short_press),
                    onSelect = { onSelectClickType(ClickType.SHORT_PRESS) },
                )
            }
            if (clickTypes.contains(ClickType.LONG_PRESS)) {
                RadioButtonText(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.LONG_PRESS,
                    text = stringResource(R.string.radio_button_long_press),
                    onSelect = { onSelectClickType(ClickType.LONG_PRESS) },
                )
            }
            if (clickTypes.contains(ClickType.DOUBLE_PRESS)) {
                RadioButtonText(
                    modifier = Modifier.weight(1f),
                    isSelected = checkedClickType == ClickType.DOUBLE_PRESS,
                    text = stringResource(R.string.radio_button_double_press),
                    onSelect = { onSelectClickType(ClickType.DOUBLE_PRESS) },
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
            RadioButtonText(
                modifier = Modifier.weight(1f),
                isSelected = mode is TriggerMode.Parallel,
                isEnabled = isEnabled,
                text = stringResource(R.string.radio_button_parallel),
                onSelect = onSelectParallelMode,
            )
            RadioButtonText(
                modifier = Modifier.weight(1f),
                isSelected = mode == TriggerMode.Sequence,
                isEnabled = isEnabled,
                text = stringResource(R.string.radio_button_sequence),
                onSelect = onSelectSequenceMode,
            )
        }
    }
}

@Composable
private fun RadioButtonText(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isEnabled: Boolean = true,
    text: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = modifier.clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            enabled = isEnabled,
        )
        Text(
            text = text,
            style = if (isEnabled) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.surfaceVariant)
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
    }
}

@Preview(device = Devices.PIXEL)
@Composable
fun VerticalPreview() {
    val sampleList = listOf(
        TriggerKeyListItemModel(
            id = "vol_up",
            name = "Volume Up",
            clickTypeString = "Long Press",
            extraInfo = "External Keyboard",
            linkType = TriggerKeyLinkType.HIDDEN,
        ),
        TriggerKeyListItemModel(
            id = "vol_down",
            name = "Volume Down",
            clickTypeString = "Single Press",
            extraInfo = "Built-in Keyboard",
            linkType = TriggerKeyLinkType.PLUS,
        ),
    )
    KeyMapperTheme {
        TriggerScreenVertical(
            configState = ConfigTriggerState(
                triggerKeys = sampleList,
                errors = listOf(
                    TextListItem.Error(
                        id = "error",
                        text = stringResource(R.string.trigger_error_dnd_access_denied),
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
            ),
            recordTriggerState = RecordTriggerState.Idle,
        )
    }
}
