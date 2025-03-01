package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowHeightSizeClass
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.mappings.ClickType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(modifier: Modifier = Modifier, viewModel: ConfigTriggerViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.setupGuiKeyboardState.collectAsStateWithLifecycle()

    if (viewModel.showDpadTriggerSetupBottomSheet) {
        DpadTriggerSetupBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                viewModel.showDpadTriggerSetupBottomSheet = false
            },
            guiKeyboardState = state,
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
}

@Composable
private fun isHorizontalLayout(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    return windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT
}

// TODO handle horizontal layout
@Composable
private fun TriggerScreen(
    modifier: Modifier = Modifier,
    isHorizontalLayout: Boolean = false,
    triggerList: List<TriggerKeyListItemState>,
    onRemoveClick: (TriggerKeyListItemState) -> Unit = {},
    onEditClick: (TriggerKeyListItemState) -> Unit = {},
) {
    Column(modifier = modifier) {
        TriggerList(
            triggerList = triggerList,
            onEditClick = onEditClick,
            onRemoveClick = onRemoveClick,
        )
    }
}

@Composable
private fun TriggerList(
    modifier: Modifier = Modifier,
    triggerList: List<TriggerKeyListItemState>,
    onRemoveClick: (TriggerKeyListItemState) -> Unit,
    onEditClick: (TriggerKeyListItemState) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(triggerList) { trigger ->
            TriggerKeyListItem(
                model = trigger,
                onRemoveClick = { onRemoveClick(trigger) },
                onEditClick = { onEditClick(trigger) },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun ClickTypeRadioGroup(
    modifier: Modifier = Modifier,
    selectedClickType: ClickType,
    onSelectClickType: (ClickType) -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.press_dot_dot_dot),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                RadioButton(
                    selected = selectedClickType == ClickType.SHORT_PRESS,
                    onClick = { onSelectClickType(ClickType.SHORT_PRESS) },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.radio_button_short_press))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                RadioButton(
                    selected = selectedClickType == ClickType.LONG_PRESS,
                    onClick = { onSelectClickType(ClickType.LONG_PRESS) },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.radio_button_long_press))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                RadioButton(
                    selected = selectedClickType == ClickType.DOUBLE_PRESS,
                    onClick = { onSelectClickType(ClickType.DOUBLE_PRESS) },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.radio_button_double_press))
            }
        }
    }
}

@Composable
private fun TriggerModeRadioGroup(
    modifier: Modifier = Modifier,
    mode: TriggerMode,
    onSelectParallelMode: () -> Unit,
    onSelectSequenceMode: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                RadioButton(
                    selected = mode is TriggerMode.Parallel,
                    onClick = onSelectParallelMode,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.radio_button_parallel))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                RadioButton(
                    selected = mode == TriggerMode.Sequence,
                    onClick = onSelectSequenceMode,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.radio_button_sequence))
            }
        }
    }
}

@Preview
@Composable
fun TriggerScreenPreview() {
    val sampleList = listOf(
        TriggerKeyListItemState(
            id = "vol_up",
            name = "Volume Up",
            clickTypeString = "Long Press",
            extraInfo = "External Keyboard",
            linkType = TriggerKeyLinkType.ARROW,
            isDragDropEnabled = true,
        ),
        TriggerKeyListItemState(
            id = "vol_down",
            name = "Volume Down",
            clickTypeString = "Single Press",
            extraInfo = "Built-in Keyboard",
            linkType = TriggerKeyLinkType.PLUS,
            isDragDropEnabled = false,
        ),
    )
    KeyMapperTheme {
        TriggerScreen(
            triggerList = sampleList,
        )
    }
}
