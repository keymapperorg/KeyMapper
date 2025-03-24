package io.github.sds100.keymapper.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.util.ui.SliderMaximums
import io.github.sds100.keymapper.util.ui.SliderMinimums
import io.github.sds100.keymapper.util.ui.SliderStepSizes
import io.github.sds100.keymapper.util.ui.compose.CheckBoxText
import io.github.sds100.keymapper.util.ui.compose.RadioButtonText
import io.github.sds100.keymapper.util.ui.compose.SliderOptionText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionOptionsBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: ActionOptionsState,
    onDismissRequest: () -> Unit = {},
    callback: ActionOptionsBottomSheetCallback,
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        val uriHandler = LocalUriHandler.current
        val helpUrl = stringResource(R.string.url_keymap_action_options_guide)
        val scope = rememberCoroutineScope()
        val sliderDefaultText = stringResource(R.string.slider_default)

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.action_options_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 8.dp),
                    onClick = { uriHandler.openUri(helpUrl) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                if (state.showEditButton) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = callback::onEditClick,
                    ) {
                        Text(stringResource(R.string.button_edit_action))
                    }
                    Spacer(Modifier.width(16.dp))
                }

                OutlinedButton(modifier = Modifier.weight(1f), onClick = callback::onReplaceClick) {
                    Text(stringResource(R.string.button_replace_action))
                }
            }

            if (state.showRepeat) {
                Spacer(Modifier.height(8.dp))

                CheckBoxText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.flag_repeat_actions),
                    isChecked = state.isRepeatChecked,
                    onCheckedChange = callback::onRepeatCheckedChange,
                )
            }

            if (state.showRepeatRate) {
                Spacer(Modifier.height(8.dp))

                SliderOptionText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.extra_label_repeat_rate),
                    defaultValue = state.defaultRepeatRate.toFloat(),
                    value = state.repeatRate.toFloat(),
                    valueText = { "${it.toInt()} ms" },
                    onValueChange = { callback.onRepeatRateChanged(it.toInt()) },
                    valueRange = 0f..SliderMaximums.ACTION_REPEAT_RATE.toFloat(),
                    stepSize = SliderStepSizes.ACTION_REPEAT_RATE,
                )
            }

            if (state.showRepeatLimit) {
                Spacer(Modifier.height(8.dp))

                val noLimitString = stringResource(R.string.button_slider_repeat_no_limit)

                // TODO fix use placeholder text for the default value instead of showing big number.
                SliderOptionText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.extra_label_repeat_limit),
                    defaultValue = state.defaultRepeatLimit.toFloat(),
                    value = state.repeatLimit.toFloat(),
                    valueText = { value ->
                        if (value.toInt() == Int.MAX_VALUE) {
                            noLimitString
                        } else {
                            "${value.toInt()}x"
                        }
                    },
                    onValueChange = { callback.onRepeatLimitChanged(it.toInt()) },
                    valueRange = 1f..SliderMaximums.ACTION_REPEAT_LIMIT.toFloat(),
                    stepSize = SliderStepSizes.ACTION_REPEAT_LIMIT,
                )
            }

            if (state.showRepeatDelay) {
                Spacer(Modifier.height(8.dp))

                SliderOptionText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.extra_label_repeat_delay),
                    defaultValue = state.defaultRepeatDelay.toFloat(),
                    value = state.repeatDelay.toFloat(),
                    valueText = { "${it.toInt()} ms" },
                    onValueChange = { callback.onRepeatDelayChanged(it.toInt()) },
                    valueRange = 0f..SliderMaximums.ACTION_REPEAT_DELAY.toFloat(),
                    stepSize = SliderStepSizes.ACTION_REPEAT_DELAY,
                )
            }

            if (state.allowedRepeatModes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.stop_repeating_dot_dot_dot),
                    style = MaterialTheme.typography.titleSmall,
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.allowedRepeatModes.contains(RepeatMode.TRIGGER_RELEASED)) {
                        RadioButtonText(
                            isSelected = state.repeatMode == RepeatMode.TRIGGER_RELEASED,
                            text = stringResource(R.string.stop_repeating_when_trigger_released),
                            onSelected = { callback.onSelectRepeatMode(RepeatMode.TRIGGER_RELEASED) },
                        )
                    }

                    if (state.allowedRepeatModes.contains(RepeatMode.TRIGGER_PRESSED_AGAIN)) {
                        RadioButtonText(
                            isSelected = state.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN,
                            text = stringResource(R.string.stop_repeating_trigger_pressed_again),
                            onSelected = { callback.onSelectRepeatMode(RepeatMode.TRIGGER_PRESSED_AGAIN) },
                        )
                    }

                    if (state.allowedRepeatModes.contains(RepeatMode.LIMIT_REACHED)) {
                        RadioButtonText(
                            isSelected = state.repeatMode == RepeatMode.LIMIT_REACHED,
                            text = stringResource(R.string.stop_repeating_limit_reached),
                            onSelected = { callback.onSelectRepeatMode(RepeatMode.LIMIT_REACHED) },
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                }
            }

            if (state.showRepeat) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            if (state.showHoldDown) {
                Spacer(Modifier.height(8.dp))

                CheckBoxText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.flag_hold_down),
                    isChecked = state.isHoldDownChecked,
                    onCheckedChange = callback::onHoldDownCheckedChange,
                )
            }

            if (state.showHoldDownDuration) {
                Spacer(Modifier.height(8.dp))

                SliderOptionText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.extra_label_hold_down_duration),
                    defaultValue = state.defaultHoldDownDuration.toFloat(),
                    value = state.holdDownDuration.toFloat(),
                    valueText = { "${it.toInt()} ms" },
                    onValueChange = { callback.onHoldDownDurationChanged(it.toInt()) },
                    valueRange = SliderMinimums.ACTION_HOLD_DOWN_DURATION.toFloat()..SliderMaximums.ACTION_HOLD_DOWN_DURATION.toFloat(),
                    stepSize = SliderStepSizes.ACTION_HOLD_DOWN_DURATION,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state.showHoldDown) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            if (state.showDelayBeforeNextAction) {
                Spacer(Modifier.height(8.dp))

                SliderOptionText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.extra_label_delay_before_next_action),
                    defaultValue = state.defaultDelayBeforeNextAction.toFloat(),
                    value = state.delayBeforeNextAction.toFloat(),
                    valueText = { "${it.toInt()} ms" },
                    onValueChange = { callback.onDelayBeforeNextActionChanged(it.toInt()) },
                    valueRange = SliderMinimums.DELAY_BEFORE_NEXT_ACTION.toFloat()..SliderMaximums.DELAY_BEFORE_NEXT_ACTION.toFloat(),
                    stepSize = SliderStepSizes.DELAY_BEFORE_NEXT_ACTION,
                )
            }

            Spacer(Modifier.height(8.dp))

            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = if (state.showRepeat && state.isRepeatChecked) {
                    stringResource(R.string.extra_label_action_multiplier_with_repeat)
                } else {
                    stringResource(R.string.extra_label_action_multiplier)
                },
                defaultValue = state.defaultMultiplier.toFloat(),
                value = state.multiplier.toFloat(),
                valueText = { "${it.toInt()}x" },
                onValueChange = { callback.onMultiplierChanged(it.toInt()) },
                valueRange = SliderMinimums.ACTION_MULTIPLIER.toFloat()..SliderMaximums.ACTION_MULTIPLIER.toFloat(),
                stepSize = SliderStepSizes.ACTION_MULTIPLIER,
            )

            Spacer(Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.weight(0.5f))
                Spacer(Modifier.width(16.dp))

                FilledTonalButton(
                    modifier = Modifier.weight(0.5f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.button_done))
                    Spacer(Modifier.width(16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

interface ActionOptionsBottomSheetCallback {
    fun onEditClick() = run { }
    fun onReplaceClick() = run { }
    fun onRepeatCheckedChange(checked: Boolean) = run { }
    fun onSelectRepeatMode(repeatMode: RepeatMode) = run { }
    fun onRepeatRateChanged(rate: Int) = run { }
    fun onRepeatLimitChanged(limit: Int) = run { }
    fun onRepeatDelayChanged(delay: Int) = run { }
    fun onHoldDownCheckedChange(checked: Boolean) = run { }
    fun onHoldDownDurationChanged(duration: Int) = run { }
    fun onDelayBeforeNextActionChanged(delay: Int) = run { }
    fun onMultiplierChanged(multiplier: Int) = run { }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(heightDp = 1000)
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = Expanded,
        )

        ActionOptionsBottomSheet(
            sheetState = sheetState,
            state = ActionOptionsState(
                showEditButton = true,
                showRepeat = true,
                isRepeatChecked = true,

                showRepeatRate = true,
                repeatRate = 400,
                defaultRepeatRate = 500,

                showRepeatDelay = true,
                repeatDelay = 400,
                defaultRepeatDelay = 400,

                showRepeatLimit = true,
                repeatLimit = Int.MAX_VALUE,
                defaultRepeatLimit = Int.MAX_VALUE,

                allowedRepeatModes = setOf(
                    RepeatMode.TRIGGER_RELEASED,
                    RepeatMode.LIMIT_REACHED,
                    RepeatMode.TRIGGER_PRESSED_AGAIN,
                ),
                repeatMode = RepeatMode.TRIGGER_RELEASED,

                showHoldDown = true,
                isHoldDownChecked = false,

                showHoldDownDuration = true,
                holdDownDuration = 400,
                defaultHoldDownDuration = 400,

                showHoldDownMode = true,
                holdDownMode = HoldDownMode.TRIGGER_PRESSED_AGAIN,

                showDelayBeforeNextAction = true,
                delayBeforeNextAction = 10000,
                defaultDelayBeforeNextAction = 5000,

                multiplier = 4,
                defaultMultiplier = 1,
            ),
            callback = object : ActionOptionsBottomSheetCallback {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewNoEditButton() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = Expanded,
        )

        ActionOptionsBottomSheet(
            sheetState = sheetState,
            state = ActionOptionsState(
                showEditButton = false,
                showRepeat = true,
                isRepeatChecked = true,

                showRepeatRate = true,
                repeatRate = 400,
                defaultRepeatRate = 500,

                showRepeatDelay = true,
                repeatDelay = 400,
                defaultRepeatDelay = 400,

                showRepeatLimit = true,
                repeatLimit = 10,
                defaultRepeatLimit = Int.MAX_VALUE,

                allowedRepeatModes = setOf(
                    RepeatMode.TRIGGER_RELEASED,
                    RepeatMode.LIMIT_REACHED,
                    RepeatMode.TRIGGER_PRESSED_AGAIN,
                ),
                repeatMode = RepeatMode.TRIGGER_RELEASED,

                showHoldDown = true,
                isHoldDownChecked = false,

                showHoldDownDuration = true,
                holdDownDuration = 400,
                defaultHoldDownDuration = 400,

                showHoldDownMode = true,
                holdDownMode = HoldDownMode.TRIGGER_PRESSED_AGAIN,

                showDelayBeforeNextAction = true,
                delayBeforeNextAction = 10000,
                defaultDelayBeforeNextAction = 5000,

                multiplier = 4,
                defaultMultiplier = 1,
            ),
            callback = object : ActionOptionsBottomSheetCallback {},
        )
    }
}
