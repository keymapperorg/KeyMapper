package io.github.sds100.keymapper.base.actions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
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
    val titleRes = when (state.actionId) {
        ActionId.DECREASE_BRIGHTNESS -> R.string.action_decrease_brightness
        else -> R.string.action_increase_brightness
    }

    KMBottomSheet(
        title = stringResource(titleRes),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = onDoneClick,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        SliderOptionText(
            title = stringResource(R.string.brightness_step_percent_label),
            value = state.stepPercent.toFloat(),
            defaultValue = DEFAULT_BRIGHTNESS_STEP_PERCENT.toFloat(),
            valueText = { "${it.roundToInt()}%" },
            onValueChange = { onStepPercentChange(it.roundToInt()) },
            valueRange = MIN_STEP_PERCENT.toFloat()..MAX_STEP_PERCENT.toFloat(),
            stepSize = STEP_PERCENT_STEP_SIZE,
        )
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
