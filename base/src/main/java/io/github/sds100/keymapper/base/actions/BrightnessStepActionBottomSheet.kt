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
import androidx.compose.material3.SheetValue
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
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val MIN_STEP_PERCENT = 1
private const val MAX_STEP_PERCENT = 50
private const val STEP_PERCENT_STEP_SIZE = 1
internal const val DEFAULT_BRIGHTNESS_STEP_PERCENT = 10

data class BrightnessStepActionBottomSheetState(
    val actionId: ActionId,
    val stepPercent: Int = DEFAULT_BRIGHTNESS_STEP_PERCENT,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessStepActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.brightnessStepActionState != null) {
        BrightnessStepActionBottomSheet(
            sheetState = sheetState,
            state = delegate.brightnessStepActionState!!,
            onDismissRequest = {
                delegate.brightnessStepActionState = null
            },
            onStepPercentChange = delegate::onBrightnessStepPercentChange,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneBrightnessStepClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrightnessStepActionBottomSheet(
    sheetState: SheetState,
    state: BrightnessStepActionBottomSheetState,
    onDismissRequest: () -> Unit = {},
    onStepPercentChange: (Int) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val titleRes = when (state.actionId) {
        ActionId.DECREASE_BRIGHTNESS -> R.string.action_decrease_brightness
        else -> R.string.action_increase_brightness
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
                style = MaterialTheme.typography.titleLarge,
            )

            SliderOptionText(
                title = stringResource(R.string.brightness_step_percent_label),
                value = state.stepPercent.toFloat(),
                defaultValue = DEFAULT_BRIGHTNESS_STEP_PERCENT.toFloat(),
                valueText = { "${it.roundToInt()}%" },
                onValueChange = { onStepPercentChange(it.roundToInt()) },
                valueRange = MIN_STEP_PERCENT.toFloat()..MAX_STEP_PERCENT.toFloat(),
                stepSize = STEP_PERCENT_STEP_SIZE,
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
@Preview(showSystemUi = true)
@Composable
private fun BrightnessStepActionBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        BrightnessStepActionBottomSheet(
            sheetState = sheetState,
            state = BrightnessStepActionBottomSheetState(
                actionId = ActionId.INCREASE_BRIGHTNESS,
                stepPercent = 25,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun BrightnessStepActionBottomSheetDefaultPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        BrightnessStepActionBottomSheet(
            sheetState = sheetState,
            state = BrightnessStepActionBottomSheetState(
                actionId = ActionId.DECREASE_BRIGHTNESS,
            ),
        )
    }
}
