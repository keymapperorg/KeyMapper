package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.VibrateEffectStrings
import io.github.sds100.keymapper.base.utils.ui.SliderMaximums
import io.github.sds100.keymapper.base.utils.ui.SliderMinimums
import io.github.sds100.keymapper.base.utils.ui.SliderStepSizes
import io.github.sds100.keymapper.base.vibration.VibrateEffect
import kotlin.math.roundToInt

enum class VibrationEffectMode {
    DURATION,
    PREDEFINED,
}

/**
 * The content for picking a [VibrateEffect]: either a custom duration or a predefined haptic
 * effect. This has no [androidx.compose.material3.ModalBottomSheet] or Done/Cancel buttons of
 * its own so it can be embedded in different bottom sheets.
 */
@Composable
fun VibrationEffectPickerContent(
    modifier: Modifier = Modifier,
    mode: VibrationEffectMode,
    onModeChange: (VibrationEffectMode) -> Unit,
    durationMs: Int,
    onDurationChange: (Int) -> Unit,
    defaultDurationMs: Int,
    predefinedType: VibrateEffect.PredefinedType,
    onPredefinedTypeChange: (VibrateEffect.PredefinedType) -> Unit,
    isPredefinedSupported: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        KeyMapperSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
            buttonStates = listOf(
                VibrationEffectMode.DURATION to
                    stringResource(R.string.vibration_effect_mode_duration),
                VibrationEffectMode.PREDEFINED to
                    if (isPredefinedSupported) {
                        stringResource(R.string.vibration_effect_mode_predefined)
                    } else {
                        stringResource(
                            R.string.vibration_effect_mode_predefined_unsuppored,
                        )
                    },
            ),
            selectedState = mode,
            onStateSelected = onModeChange,
            isStateEnabled = { it != VibrationEffectMode.PREDEFINED || isPredefinedSupported },
        )

        when (mode) {
            VibrationEffectMode.DURATION -> {
                val durationMin = SliderMinimums.VIBRATION_DURATION
                val durationMax = SliderMaximums.VIBRATION_DURATION

                SliderOptionText(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.extra_label_vibration_duration),
                    value = durationMs.toFloat(),
                    defaultValue = defaultDurationMs.toFloat(),
                    valueText = { "${it.toInt()} ms" },
                    onValueChange = { onDurationChange(it.roundToInt()) },
                    valueRange = durationMin.toFloat()..durationMax.toFloat(),
                    stepSize = SliderStepSizes.VIBRATION_DURATION,
                )
            }

            VibrationEffectMode.PREDEFINED -> {
                FlowRow(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (type in VibrateEffect.PredefinedType.entries) {
                        FilterChip(
                            selected = predefinedType == type,
                            onClick = { onPredefinedTypeChange(type) },
                            label = { Text(stringResource(VibrateEffectStrings.getLabel(type))) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun VibrationEffectPickerContentDurationPreview() {
    KeyMapperTheme {
        Surface {
            VibrationEffectPickerContent(
                modifier = Modifier.fillMaxWidth(),
                mode = VibrationEffectMode.DURATION,
                onModeChange = {},
                durationMs = 200,
                onDurationChange = {},
                defaultDurationMs = 200,
                predefinedType = VibrateEffect.PredefinedType.CLICK,
                onPredefinedTypeChange = {},
            )
        }
    }
}

@Preview(widthDp = 300)
@Composable
private fun VibrationEffectPickerContentPredefinedPreview() {
    KeyMapperTheme {
        Surface {
            VibrationEffectPickerContent(
                modifier = Modifier.fillMaxWidth(),
                mode = VibrationEffectMode.PREDEFINED,
                onModeChange = {},
                durationMs = 200,
                onDurationChange = {},
                defaultDurationMs = 200,
                predefinedType = VibrateEffect.PredefinedType.CLICK,
                onPredefinedTypeChange = {},
            )
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun VibrationEffectPickerContentPredefinedUnsupportedPreview() {
    KeyMapperTheme {
        Surface {
            VibrationEffectPickerContent(
                modifier = Modifier.fillMaxWidth(),
                mode = VibrationEffectMode.DURATION,
                onModeChange = {},
                durationMs = 200,
                onDurationChange = {},
                defaultDurationMs = 200,
                predefinedType = VibrateEffect.PredefinedType.CLICK,
                onPredefinedTypeChange = {},
                isPredefinedSupported = false,
            )
        }
    }
}
