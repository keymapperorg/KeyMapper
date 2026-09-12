package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val MIN_DURATION_SECONDS = 5
private const val MAX_DURATION_SECONDS = 60
private const val DURATION_STEP_SECONDS = 5

data class StepMediaActionBottomSheetState(
    val actionId: ActionId,
    val packageName: String? = null,
    val durationEnabled: Boolean = false,
    /**
     * UI works with seconds for user-friendliness
     */
    val durationSeconds: Int = 30,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepMediaActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.stepMediaActionBottomSheetState != null) {
        StepMediaActionBottomSheet(
            sheetState = sheetState,
            state = delegate.stepMediaActionBottomSheetState!!,
            onDismissRequest = {
                delegate.stepMediaActionBottomSheetState = null
            },
            onDurationEnabledChange = delegate::onStepMediaDurationEnabledChange,
            onDurationChange = delegate::onStepMediaDurationChange,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneStepMediaClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepMediaActionBottomSheet(
    sheetState: SheetState,
    state: StepMediaActionBottomSheetState,
    onDismissRequest: () -> Unit = {},
    onDurationEnabledChange: (Boolean) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val titleRes = when (state.actionId) {
        ActionId.STEP_BACKWARD, ActionId.STEP_BACKWARD_PACKAGE ->
            R.string.action_step_backward_media

        else -> R.string.action_step_forward_media
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center,
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineMedium,
            )

            CheckBoxText(
                text = stringResource(R.string.action_step_media_duration_checkbox),
                isChecked = state.durationEnabled,
                onCheckedChange = onDurationEnabledChange,
            )

            if (state.durationEnabled) {
                val durationValueFormat =
                    stringResource(R.string.action_step_media_duration_value)

                SliderOptionText(
                    title = stringResource(R.string.action_step_media_duration_label),
                    value = state.durationSeconds.toFloat(),
                    defaultValue = 30f,
                    valueText = { value ->
                        durationValueFormat.format(value.roundToInt())
                    },
                    onValueChange = { onDurationChange(it.roundToInt()) },
                    valueRange = MIN_DURATION_SECONDS.toFloat()..MAX_DURATION_SECONDS.toFloat(),
                    stepSize = DURATION_STEP_SECONDS,
                )
            }

            Text(
                text = stringResource(R.string.action_step_media_duration_not_supported_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun StepMediaActionBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        StepMediaActionBottomSheet(
            sheetState = sheetState,
            state = StepMediaActionBottomSheetState(
                actionId = ActionId.STEP_FORWARD,
                durationEnabled = true,
                durationSeconds = 45,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun StepMediaActionBottomSheetDefaultPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        StepMediaActionBottomSheet(
            sheetState = sheetState,
            state = StepMediaActionBottomSheetState(
                actionId = ActionId.STEP_BACKWARD,
                durationEnabled = false,
            ),
        )
    }
}
