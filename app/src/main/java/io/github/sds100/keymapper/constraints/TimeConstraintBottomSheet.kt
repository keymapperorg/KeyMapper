package io.github.sds100.keymapper.constraints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.util.ui.compose.OptionsHeaderRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeConstraintBottomSheet(viewModel: ChooseConstraintViewModel) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (viewModel.timeConstraintState != null) {
        TimeConstraintBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                viewModel.timeConstraintState = null
            },
            state = viewModel.timeConstraintState!!,
            onSelectStartTime = { hour, min ->
                viewModel.timeConstraintState = viewModel.timeConstraintState?.copy(
                    startHour = hour,
                    startMinute = min,
                )
            },
            onSelectEndTime = { hour, min ->
                viewModel.timeConstraintState = viewModel.timeConstraintState?.copy(
                    endHour = hour,
                    endMinute = min,
                )
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    viewModel.onDoneConfigTimeConstraintClick()
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
    state: Constraint.Time,
    onSelectStartTime: (Int, Int) -> Unit = { _, _ -> },
    onSelectEndTime: (Int, Int) -> Unit = { _, _ -> },
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.constraint_time_bottom_sheet_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OptionsHeaderRow(
                icon = Icons.Rounded.Clo
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.neg_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDoneClick,
                ) {
                    Text(stringResource(R.string.pos_done))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        TimeConstraintBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = Constraint.Time(
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
