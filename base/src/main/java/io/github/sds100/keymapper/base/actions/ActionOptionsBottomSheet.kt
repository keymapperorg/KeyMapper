package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.SliderMaximums
import io.github.sds100.keymapper.base.utils.ui.SliderMinimums
import io.github.sds100.keymapper.base.utils.ui.SliderStepSizes
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import io.github.sds100.keymapper.base.utils.ui.compose.TextFieldDialog
import io.github.sds100.keymapper.base.utils.ui.compose.openUriSafe
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
        val ctx = LocalContext.current
        val helpUrl = stringResource(R.string.url_keymap_action_options_guide)
        val scope = rememberCoroutineScope()
        var showCustomNameDialog by rememberSaveable { mutableStateOf(false) }

        if (showCustomNameDialog) {
            TextFieldDialog(
                title = stringResource(R.string.action_options_custom_name_dialog_title),
                submitButtonText = stringResource(R.string.pos_save),
                initialText = state.title,
                onSubmitClick = { newText ->
                    callback.onCustomNameChanged(newText)
                    null
                },
                onDismissRequest = { showCustomNameDialog = false },
            )
        }
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        textAlign = TextAlign.Center,
                        text = state.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )

                    IconButton(onClick = { showCustomNameDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(
                                R.string.action_options_custom_name_dialog_title,
                            ),
                        )
                    }
                }

                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = 8.dp),
                    onClick = { uriHandler.openUriSafe(ctx, helpUrl) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OptionsHeaderRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = state.actionTypeIcon,
                text = stringResource(R.string.action_options_type_header, state.actionTypeTitle),
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.showEditButton) {
                    FilledTonalButton(onClick = callback::onEditClick) {
                        Text(stringResource(R.string.action_options_customize))
                    }
                }

                FilledTonalButton(onClick = callback::onReplaceClick) {
                    Text(stringResource(R.string.action_options_swap))
                }
            }

            if (state.showRepeat) {
                Spacer(modifier = Modifier.height(16.dp))
                RepeatOptions(state = state, callback = callback)
            }

            Spacer(modifier = Modifier.height(16.dp))
            BurstOptions(state = state, callback = callback)

            if (state.showHoldDown) {
                Spacer(modifier = Modifier.height(16.dp))
                HoldDownOptions(state = state, callback = callback)
            }

            if (state.showDelayBeforeNextAction) {
                Spacer(modifier = Modifier.height(16.dp))
                DelayOptions(state = state, callback = callback)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Icon(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.button_done))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RepeatOptions(state: ActionOptionsState, callback: ActionOptionsBottomSheetCallback) {
    Column {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.Repeat,
            text = stringResource(R.string.action_options_repeat_header),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.action_options_repeat_description),
            style = MaterialTheme.typography.bodyMedium,
        )

        val selectedMode: RepeatMode? = if (state.isRepeatChecked) {
            state.repeatMode
        } else {
            null
        }

        RadioButtonText(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.action_options_dont_repeat),
            isSelected = selectedMode == null,
            onSelected = { callback.onSelectRepeatMode(null) },
        )

        RadioButtonText(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.action_options_repeat_limit_reached),
            isSelected = selectedMode == RepeatMode.LIMIT_REACHED,
            isEnabled = state.allowedRepeatModes.contains(RepeatMode.LIMIT_REACHED),
            onSelected = { callback.onSelectRepeatMode(RepeatMode.LIMIT_REACHED) },
        )

        val isUntilReleasedAllowed =
            state.allowedRepeatModes.contains(RepeatMode.TRIGGER_RELEASED)

        RadioButtonText(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.action_options_repeat_until_released),
            isSelected = selectedMode == RepeatMode.TRIGGER_RELEASED,
            isEnabled = isUntilReleasedAllowed,
            onSelected = { callback.onSelectRepeatMode(RepeatMode.TRIGGER_RELEASED) },
        )

        if (!isUntilReleasedAllowed) {
            Text(
                // Align with the text of the radio button.
                modifier = Modifier.padding(start = 48.dp, end = 16.dp),
                text = stringResource(R.string.action_options_repeat_until_released_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        RadioButtonText(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.action_options_repeat_until_pressed_again),
            isSelected = selectedMode == RepeatMode.TRIGGER_PRESSED_AGAIN,
            isEnabled = state.allowedRepeatModes.contains(RepeatMode.TRIGGER_PRESSED_AGAIN),
            onSelected = { callback.onSelectRepeatMode(RepeatMode.TRIGGER_PRESSED_AGAIN) },
        )

        if (state.showRepeatRateWarning) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_repeat_rate_warning),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        if (state.showRepeatDelay) {
            Spacer(modifier = Modifier.height(8.dp))

            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.action_options_repeat_delay),
                defaultValue = state.defaultRepeatDelay.toFloat(),
                value = state.repeatDelay.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onRepeatDelayChanged(it.toInt()) },
                valueRange = 0f..SliderMaximums.ACTION_REPEAT_DELAY.toFloat(),
                stepSize = SliderStepSizes.ACTION_REPEAT_DELAY,
            )
        }

        if (state.showRepeatRate) {
            Spacer(modifier = Modifier.height(8.dp))

            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.action_options_repeat_rate),
                defaultValue = state.defaultRepeatRate.toFloat(),
                value = state.repeatRate.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onRepeatRateChanged(it.toInt()) },
                valueRange = 0f..SliderMaximums.ACTION_REPEAT_RATE.toFloat(),
                stepSize = SliderStepSizes.ACTION_REPEAT_RATE,
            )
        }

        if (state.showRepeatLimit) {
            Spacer(modifier = Modifier.height(8.dp))

            val noLimitString = stringResource(R.string.button_slider_repeat_no_limit)

            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.action_options_repeat_limit),
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
    }
}

@Composable
private fun BurstOptions(state: ActionOptionsState, callback: ActionOptionsBottomSheetCallback) {
    Column {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.KeyboardDoubleArrowRight,
            text = stringResource(R.string.action_options_burst_header),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = if (state.showRepeat && state.isRepeatChecked) {
                stringResource(R.string.action_options_burst_description_repeat)
            } else {
                stringResource(R.string.action_options_burst_description)
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val multiplierMin = SliderMinimums.ACTION_MULTIPLIER.toFloat()
        val multiplierMax = SliderMaximums.ACTION_MULTIPLIER.toFloat()
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.action_options_burst_size),
            defaultValue = state.defaultMultiplier.toFloat(),
            value = state.multiplier.toFloat(),
            valueText = { "${it.toInt()}x" },
            onValueChange = { callback.onMultiplierChanged(it.toInt()) },
            valueRange = multiplierMin..multiplierMax,
            stepSize = SliderStepSizes.ACTION_MULTIPLIER,
        )
    }
}

@Composable
private fun HoldDownOptions(state: ActionOptionsState, callback: ActionOptionsBottomSheetCallback) {
    val isRepeating = state.showRepeat && state.isRepeatChecked

    Column {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.TouchApp,
            text = stringResource(R.string.action_options_hold_down_header),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = if (isRepeating) {
                stringResource(R.string.action_options_hold_down_description_repeat)
            } else {
                stringResource(R.string.action_options_hold_down_description)
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        val selectedMode: HoldDownMode? = if (state.isHoldDownChecked) {
            state.holdDownMode
        } else {
            null
        }

        RadioButtonText(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.action_options_dont_hold_down),
            isSelected = selectedMode == null,
            onSelected = { callback.onSelectHoldDownMode(null) },
        )

        if (isRepeating) {
            // When repeating, the action is held down for a period of time before each repeat.
            RadioButtonText(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.action_options_hold_down_period),
                isSelected = selectedMode != null,
                onSelected = { callback.onSelectHoldDownMode(HoldDownMode.TRIGGER_RELEASED) },
            )

            if (state.showHoldDownDuration) {
                Spacer(modifier = Modifier.height(8.dp))

                val holdDownDurationMin = SliderMinimums.ACTION_HOLD_DOWN_DURATION.toFloat()
                val holdDownDurationMax = SliderMaximums.ACTION_HOLD_DOWN_DURATION.toFloat()
                SliderOptionText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.extra_label_hold_down_duration),
                    defaultValue = state.defaultHoldDownDuration.toFloat(),
                    value = state.holdDownDuration.toFloat(),
                    valueText = { "${it.toInt()} ms" },
                    onValueChange = { callback.onHoldDownDurationChanged(it.toInt()) },
                    valueRange = holdDownDurationMin..holdDownDurationMax,
                    stepSize = SliderStepSizes.ACTION_HOLD_DOWN_DURATION,
                )
            }
        } else {
            RadioButtonText(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.action_options_hold_down_until_released),
                isSelected = selectedMode == HoldDownMode.TRIGGER_RELEASED,
                onSelected = { callback.onSelectHoldDownMode(HoldDownMode.TRIGGER_RELEASED) },
            )

            RadioButtonText(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.action_options_hold_down_until_pressed_again),
                isSelected = selectedMode == HoldDownMode.TRIGGER_PRESSED_AGAIN,
                onSelected = {
                    callback.onSelectHoldDownMode(HoldDownMode.TRIGGER_PRESSED_AGAIN)
                },
            )
        }
    }
}

@Composable
private fun DelayOptions(state: ActionOptionsState, callback: ActionOptionsBottomSheetCallback) {
    Column {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.HourglassEmpty,
            text = stringResource(R.string.action_options_delay_header),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val delayMin = SliderMinimums.DELAY_BEFORE_NEXT_ACTION.toFloat()
        val delayMax = SliderMaximums.DELAY_BEFORE_NEXT_ACTION.toFloat()
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.extra_label_delay_before_next_action),
            defaultValue = state.defaultDelayBeforeNextAction.toFloat(),
            value = state.delayBeforeNextAction.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { callback.onDelayBeforeNextActionChanged(it.toInt()) },
            valueRange = delayMin..delayMax,
            stepSize = SliderStepSizes.DELAY_BEFORE_NEXT_ACTION,
        )
    }
}

interface ActionOptionsBottomSheetCallback {
    fun onEditClick() = run { }
    fun onReplaceClick() = run { }
    fun onCustomNameChanged(name: String) = run { }

    /**
     * @param repeatMode null if the action should not repeat.
     */
    fun onSelectRepeatMode(repeatMode: RepeatMode?) = run { }
    fun onRepeatRateChanged(rate: Int) = run { }
    fun onRepeatLimitChanged(limit: Int) = run { }
    fun onRepeatDelayChanged(delay: Int) = run { }

    /**
     * @param holdDownMode null if the action should not be held down.
     */
    fun onSelectHoldDownMode(holdDownMode: HoldDownMode?) = run { }
    fun onHoldDownDurationChanged(duration: Int) = run { }
    fun onDelayBeforeNextActionChanged(delay: Int) = run { }
    fun onMultiplierChanged(multiplier: Int) = run { }
}

private val previewState = ActionOptionsState(
    title = "Input KEYCODE_0",
    actionTypeTitle = "Input key event",
    actionTypeIcon = Icons.Rounded.Abc,

    showEditButton = true,
    showRepeat = true,
    isRepeatChecked = true,
    showRepeatRateWarning = false,

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
    repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,

    showHoldDown = true,
    isHoldDownChecked = true,

    showHoldDownDuration = true,
    holdDownDuration = 400,
    defaultHoldDownDuration = 400,

    holdDownMode = HoldDownMode.TRIGGER_RELEASED,

    showDelayBeforeNextAction = true,
    delayBeforeNextAction = 10000,
    defaultDelayBeforeNextAction = 5000,

    multiplier = 4,
    defaultMultiplier = 1,
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(heightDp = 1600, showSystemUi = true)
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        ActionOptionsBottomSheet(
            sheetState = sheetState,
            state = previewState,
            callback = object : ActionOptionsBottomSheetCallback {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(heightDp = 1600, showSystemUi = true)
@Composable
private fun PreviewUntilReleasedUnavailable() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        ActionOptionsBottomSheet(
            sheetState = sheetState,
            state = previewState.copy(
                showEditButton = false,
                showRepeatRateWarning = true,
                allowedRepeatModes = setOf(
                    RepeatMode.LIMIT_REACHED,
                    RepeatMode.TRIGGER_PRESSED_AGAIN,
                ),
                isHoldDownChecked = false,
            ),
            callback = object : ActionOptionsBottomSheetCallback {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(heightDp = 1600, showSystemUi = true)
@Composable
private fun PreviewNotRepeating() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
            skipHiddenState = true,
        )

        ActionOptionsBottomSheet(
            sheetState = sheetState,
            state = previewState.copy(
                isRepeatChecked = false,
                showRepeatRate = false,
                showRepeatDelay = false,
                showRepeatLimit = false,
                showHoldDownDuration = false,
                holdDownMode = HoldDownMode.TRIGGER_PRESSED_AGAIN,
            ),
            callback = object : ActionOptionsBottomSheetCallback {},
        )
    }
}
