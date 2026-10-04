package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
import io.github.sds100.keymapper.base.utils.ui.compose.VibrationEffectMode
import io.github.sds100.keymapper.base.utils.ui.compose.VibrationEffectPickerContent
import io.github.sds100.keymapper.base.vibration.VibrateConfigState
import io.github.sds100.keymapper.base.vibration.VibrateEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrateActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val state = delegate.vibrateConfigState

    if (state != null) {
        VibrateActionBottomSheet(
            sheetState = sheetState,
            state = state,
            onDismissRequest = delegate::closeVibrateConfig,
            onModeChange = delegate::onVibrateModeChange,
            onDurationChange = delegate::onVibrateDurationChange,
            onPredefinedTypeChange = delegate::onVibratePredefinedTypeChange,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneVibrateClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VibrateActionBottomSheet(
    sheetState: SheetState,
    state: VibrateConfigState,
    onDismissRequest: () -> Unit = {},
    onModeChange: (VibrationEffectMode) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onPredefinedTypeChange: (VibrateEffect.PredefinedType) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    KMBottomSheet(
        title = stringResource(R.string.action_vibrate),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = onDoneClick,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        VibrationEffectPickerContent(
            modifier = Modifier.fillMaxWidth(),
            mode = state.mode,
            onModeChange = onModeChange,
            durationMs = state.durationMs,
            onDurationChange = onDurationChange,
            defaultDurationMs = state.defaultDurationMs,
            predefinedType = state.predefinedType,
            onPredefinedTypeChange = onPredefinedTypeChange,
            isPredefinedSupported = state.isPredefinedSupported,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun VibrateActionBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        VibrateActionBottomSheet(
            sheetState = sheetState,
            state = VibrateConfigState(durationMs = 200, defaultDurationMs = 200),
        )
    }
}
