package io.github.sds100.keymapper.base.keymaps

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.VibrationEffectMode
import io.github.sds100.keymapper.base.utils.ui.compose.VibrationEffectPickerContent
import io.github.sds100.keymapper.base.vibration.VibrateConfigState
import io.github.sds100.keymapper.base.vibration.VibrateEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerVibrationBottomSheet(viewModel: ConfigKeyMapOptionsViewModel) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val state = viewModel.vibrateConfigState

    if (state != null) {
        TriggerVibrationBottomSheet(
            sheetState = sheetState,
            state = state,
            onDismissRequest = viewModel::closeVibrateConfig,
            onModeChange = viewModel::onVibrateModeChange,
            onDurationChange = viewModel::onVibrateDurationChange,
            onPredefinedTypeChange = viewModel::onVibratePredefinedTypeChange,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    viewModel.onDoneVibrateConfigClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerVibrationBottomSheet(
    sheetState: SheetState,
    state: VibrateConfigState,
    onDismissRequest: () -> Unit = {},
    onModeChange: (VibrationEffectMode) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onPredefinedTypeChange: (VibrateEffect.PredefinedType) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

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
                text = stringResource(R.string.flag_vibrate),
                style = MaterialTheme.typography.headlineMedium,
            )

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
private fun TriggerVibrationBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        TriggerVibrationBottomSheet(
            sheetState = sheetState,
            state = VibrateConfigState(durationMs = 200, defaultDurationMs = 200),
        )
    }
}
