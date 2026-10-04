package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.common.utils.TimeUtils
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeConstraintBottomSheet(delegate: CreateConstraintDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.timeConstraintState != null) {
        TimeConstraintBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.timeConstraintState = null
            },
            state = delegate.timeConstraintState!!,
            onSelectStartTime = { hour, min ->
                delegate.timeConstraintState = delegate.timeConstraintState?.copy(
                    startHour = hour,
                    startMinute = min,
                )
            },
            onSelectEndTime = { hour, min ->
                delegate.timeConstraintState = delegate.timeConstraintState?.copy(
                    endHour = hour,
                    endMinute = min,
                )
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneConfigTimeConstraintClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeConstraintBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: ConstraintData.Time,
    onSelectStartTime: (Int, Int) -> Unit = { _, _ -> },
    onSelectEndTime: (Int, Int) -> Unit = { _, _ -> },
    onDoneClick: () -> Unit = {},
) {
    val formatter = remember { TimeUtils.localeDateFormatter(FormatStyle.SHORT) }

    val startTimePickerState = rememberTimePickerState()
    var showStartTimePickerDialog by remember { mutableStateOf(false) }

    if (showStartTimePickerDialog) {
        TimePickerDialog(
            state = startTimePickerState,
            onDismiss = {
                showStartTimePickerDialog = false
            },
            onConfirm = {
                onSelectStartTime(startTimePickerState.hour, startTimePickerState.minute)
                showStartTimePickerDialog = false
            },
        )
    }

    val endTimePickerState = rememberTimePickerState()
    var showEndTimePickerDialog by remember { mutableStateOf(false) }

    if (showEndTimePickerDialog) {
        TimePickerDialog(
            state = endTimePickerState,
            onDismiss = {
                showEndTimePickerDialog = false
            },
            onConfirm = {
                onSelectEndTime(endTimePickerState.hour, endTimePickerState.minute)
                showEndTimePickerDialog = false
            },
        )
    }

    KMBottomSheet(
        title = stringResource(R.string.constraint_time_bottom_sheet_title),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = onDoneClick,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        OptionsHeaderRow(
            icon = Icons.Rounded.Timer,
            text = stringResource(R.string.constraint_time_bottom_sheet_start_time),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatter.format(state.startTime),
                style = MaterialTheme.typography.titleLarge,
            )

            IconButton(
                modifier = Modifier.padding(start = 8.dp),
                onClick = {
                    startTimePickerState.hour = state.startHour
                    startTimePickerState.minute = state.startMinute

                    showStartTimePickerDialog = true
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(
                        R.string.constraint_time_bottom_sheet_edit_start_time,
                    ),
                )
            }
        }

        OptionsHeaderRow(
            icon = Icons.Rounded.TimerOff,
            text = stringResource(R.string.constraint_time_bottom_sheet_end_time),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatter.format(state.endTime),
                style = MaterialTheme.typography.titleLarge,
            )

            IconButton(
                modifier = Modifier.padding(start = 8.dp),
                onClick = {
                    endTimePickerState.hour = state.endHour
                    endTimePickerState.minute = state.endMinute

                    showEndTimePickerDialog = true
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(
                        R.string.constraint_time_bottom_sheet_edit_end_time,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(state: TimePickerState, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(R.string.pos_ok))
            }
        },
        text = {
            TimePicker(state = state)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        TimeConstraintBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ConstraintData.Time(
                startHour = 0,
                startMinute = 0,
                endHour = 23,
                endMinute = 59,
            ),
            onSelectStartTime = { _, _ -> },
            onSelectEndTime = { _, _ -> },
            onDoneClick = {},
        )
    }
}
