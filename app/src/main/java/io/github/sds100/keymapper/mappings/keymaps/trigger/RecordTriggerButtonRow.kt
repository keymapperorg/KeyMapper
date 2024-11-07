package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.LocalCustomColorsPalette

/**
 * This row of buttons is shown at the bottom of the TriggerFragment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTriggerButtonRow(
    modifier: Modifier = Modifier,
    viewModel: ConfigTriggerViewModel,
) {
    val recordTriggerState by viewModel.recordTriggerState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (viewModel.showAdvancedTriggersBottomSheet) {
        AdvancedTriggersBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            viewModel = viewModel,
            onDismissRequest = {
                viewModel.showAdvancedTriggersBottomSheet
            },
            sheetState = sheetState,
        )
    }

    RecordTriggerButtonRow(
        modifier = modifier,
        onRecordTriggerClick = viewModel::onRecordTriggerButtonClick,
        recordTriggerState = recordTriggerState,
        onAdvancedTriggersClick = {
            viewModel.showAdvancedTriggersBottomSheet = true
        },
    )
}

/**
 * This row of buttons is shown at the bottom of the TriggerFragment.
 */
@Composable
private fun RecordTriggerButtonRow(
    modifier: Modifier = Modifier,
    onRecordTriggerClick: () -> Unit = {},
    recordTriggerState: RecordTriggerState,
    onAdvancedTriggersClick: () -> Unit = {},
) {
    Row(modifier) {
        RecordTriggerButton(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom),
            recordTriggerState,
            onClick = onRecordTriggerClick,
        )

        Spacer(modifier = Modifier.width(8.dp))

        AdvancedTriggersButton(
            modifier = Modifier.weight(1f),
            isEnabled = recordTriggerState is RecordTriggerState.Stopped,
            onClick = onAdvancedTriggersClick,
        )
    }
}

@Composable
private fun RecordTriggerButton(
    modifier: Modifier,
    state: RecordTriggerState,
    onClick: () -> Unit,
) {
    val colors = ButtonDefaults.filledTonalButtonColors().copy(
        containerColor = LocalCustomColorsPalette.current.red,
        contentColor = LocalCustomColorsPalette.current.onRed,
    )

    val text: String = when (state) {
        is RecordTriggerState.CountingDown ->
            stringResource(R.string.button_recording_trigger_countdown, state.timeLeft)

        RecordTriggerState.Stopped ->
            stringResource(R.string.button_record_trigger)
    }

    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        colors = colors,
    ) {
        Text(text)
    }
}

@Composable
private fun AdvancedTriggersButton(
    modifier: Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            enabled = isEnabled,
            onClick = onClick,
        ) {
            Text(stringResource(R.string.button_advanced_triggers))
        }

        Badge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(36.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResource(R.string.button_advanced_triggers_badge),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewCountingDown() {
    KeyMapperTheme {
        Surface {
            RecordTriggerButtonRow(
                modifier = Modifier.fillMaxWidth(),
                recordTriggerState = RecordTriggerState.CountingDown(3),
            )
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewStopped() {
    KeyMapperTheme {
        Surface {
            RecordTriggerButtonRow(
                modifier = Modifier.fillMaxWidth(),
                recordTriggerState = RecordTriggerState.Stopped,
            )
        }
    }
}
