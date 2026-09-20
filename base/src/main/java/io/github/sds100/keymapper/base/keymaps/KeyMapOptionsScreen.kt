package io.github.sds100.keymapper.base.keymaps

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.onboarding.TipCard
import io.github.sds100.keymapper.base.utils.ui.SliderMaximums
import io.github.sds100.keymapper.base.utils.ui.SliderMinimums
import io.github.sds100.keymapper.base.utils.ui.SliderStepSizes
import io.github.sds100.keymapper.base.utils.ui.compose.OptionPageButton
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import io.github.sds100.keymapper.base.utils.ui.compose.SwitchPreferenceCompose
import io.github.sds100.keymapper.base.utils.ui.compose.TextFieldDialog
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.MobileSensorHi
import io.github.sds100.keymapper.base.utils.ui.compose.icons.Switch
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.launch

@Composable
fun KeyMapOptionsScreen(modifier: Modifier = Modifier, viewModel: ConfigKeyMapOptionsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KeyMapOptionsScreen(
        modifier = modifier,
        state = state,
        callback = viewModel,
    )

    val createShortcutDialogState = viewModel.createShortcutDialogState

    if (createShortcutDialogState != null) {
        TextFieldDialog(
            title = stringResource(R.string.key_map_options_create_shortcut_title),
            submitButtonText = stringResource(R.string.pos_save),
            initialText = createShortcutDialogState.defaultName,
            onSubmitClick = { newText -> viewModel.onConfirmCreateShortcut(newText) },
            onDismissRequest = viewModel::onDismissCreateShortcutDialog,
        )
    }
}

@Composable
fun KeyMapOptionsScreen(
    modifier: Modifier = Modifier,
    state: State<KeyMapOptionsState>,
    callback: KeyMapOptionsCallback,
) {
    when (state) {
        is State.Loading ->
            Surface(modifier) {
                Loading(Modifier.fillMaxSize())
            }

        is State.Data -> Surface(modifier) {
            Loaded(Modifier.fillMaxSize(), state.data, callback)
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Loaded(
    modifier: Modifier = Modifier,
    state: KeyMapOptionsState,
    callback: KeyMapOptionsCallback = object : KeyMapOptionsCallback {},
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (state.showScreenOffTip) {
            TipCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.tip_screen_off_trigger_title),
                message = stringResource(R.string.tip_screen_off_trigger_message),
                isDismissable = false,
                buttonText = if (state.isExpertModeStarted) {
                    null
                } else {
                    stringResource(R.string.button_enable_expert_mode)
                },
                onButtonClick = callback::onOpenExpertModeSettings,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            icon = Icons.Outlined.Info,
            text = stringResource(R.string.key_map_options_header_information),
        )

        KeyMapUidRow(
            modifier = Modifier
                .fillMaxWidth(),
            keyMapUid = state.keyMapUid,
        )

        Spacer(modifier = Modifier.height(4.dp))

        OptionsHeaderRow(
            icon = Icons.Outlined.Sensors,
            text = stringResource(R.string.key_map_options_header_feedback),
        )

        if (state.showVibrate) {
            SwitchPreferenceCompose(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.flag_vibrate),
                text = stringResource(R.string.key_map_options_vibrate_summary),
                icon = Icons.Outlined.Vibration,
                isChecked = state.vibrate,
                onCheckedChange = callback::onVibrateChanged,
            )
        }

        if (state.showVibrateDuration) {
            val vibrateDurationMin = SliderMinimums.VIBRATION_DURATION
            val vibrateDurationMax = SliderMaximums.VIBRATION_DURATION
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth(),
                title = stringResource(R.string.extra_label_vibration_duration),
                defaultValue = state.defaultVibrateDuration.toFloat(),
                value = state.vibrateDuration.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onVibrateDurationChanged(it.toInt()) },
                valueRange = vibrateDurationMin.toFloat()..vibrateDurationMax.toFloat(),
                stepSize = SliderStepSizes.VIBRATION_DURATION,
            )
        }

        if (state.showLongPressDoubleVibration) {
            SwitchPreferenceCompose(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.flag_long_press_double_vibration),
                text = null,
                icon = KeyMapperIcons.MobileSensorHi,
                isChecked = state.longPressDoubleVibration,
                onCheckedChange = callback::onLongPressDoubleVibrationChanged,
            )
        }

        SwitchPreferenceCompose(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.flag_show_toast),
            text = stringResource(R.string.key_map_options_show_toast_summary),
            icon = Icons.Outlined.Message,
            isChecked = state.showToast,
            onCheckedChange = callback::onShowToastChanged,
        )

        val showTimingSection = state.showLongPressDelay ||
            state.showDoublePressDelay ||
            state.showSequenceTriggerTimeout

        if (showTimingSection) {
            OptionsHeaderRow(
                icon = Icons.Outlined.Timer,
                text = stringResource(R.string.key_map_options_header_timing),
            )
        }

        if (state.showLongPressDelay) {
            val longPressDelayMin = SliderMinimums.TRIGGER_LONG_PRESS_DELAY
            val longPressDelayMax = SliderMaximums.TRIGGER_LONG_PRESS_DELAY
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth(),
                title = stringResource(R.string.extra_label_long_press_delay_timeout),
                defaultValue = state.defaultLongPressDelay.toFloat(),
                value = state.longPressDelay.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onLongPressDelayChanged(it.toInt()) },
                valueRange = longPressDelayMin.toFloat()..longPressDelayMax.toFloat(),
                stepSize = SliderStepSizes.TRIGGER_LONG_PRESS_DELAY,
            )
        }

        if (state.showDoublePressDelay) {
            val doublePressDelayMin = SliderMinimums.TRIGGER_DOUBLE_PRESS_DELAY
            val doublePressDelayMax = SliderMaximums.TRIGGER_DOUBLE_PRESS_DELAY
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth(),
                title = stringResource(R.string.extra_label_double_press_delay_timeout),
                defaultValue = state.defaultDoublePressDelay.toFloat(),
                value = state.doublePressDelay.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onDoublePressDelayChanged(it.toInt()) },
                valueRange = doublePressDelayMin.toFloat()..doublePressDelayMax.toFloat(),
                stepSize = SliderStepSizes.TRIGGER_DOUBLE_PRESS_DELAY,
            )
        }

        if (state.showSequenceTriggerTimeout) {
            val sequenceTriggerTimeoutMin =
                SliderMinimums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT.toFloat()
            val sequenceTriggerTimeoutMax =
                SliderMaximums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT.toFloat()
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth(),
                title = stringResource(R.string.extra_label_sequence_trigger_timeout),
                defaultValue = state.defaultSequenceTriggerTimeout.toFloat(),
                value = state.sequenceTriggerTimeout.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onSequenceTriggerTimeoutChanged(it.toInt()) },
                valueRange = sequenceTriggerTimeoutMin..sequenceTriggerTimeoutMax,
                stepSize = SliderStepSizes.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT,
            )
        }

        OptionsHeaderRow(
            icon = Icons.Outlined.Route,
            text = stringResource(R.string.key_map_options_header_integration),
        )

        OptionPageButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.key_map_options_enable_by_intent_title),
            text = stringResource(R.string.key_map_options_intent_summary),
            icon = KeyMapperIcons.Switch,
            onClick = callback::onEnableByIntentClick,
        )
        SwitchPreferenceCompose(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.key_map_options_trigger_from_other_apps_title),
            text = stringResource(R.string.key_map_options_trigger_from_other_apps_summary),
            icon = Icons.Outlined.Route,
            isChecked = state.triggerFromOtherApps,
            onCheckedChange = callback::onTriggerFromOtherAppsChanged,
        )

        OptionPageButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.key_map_options_trigger_by_intent_title),
            text = stringResource(R.string.key_map_options_intent_summary),
            icon = Icons.Outlined.RocketLaunch,
            // Key Mapper only listens for these intents when other apps are allowed to
            // control this key map.
            enabled = state.triggerFromOtherApps,
            onClick = callback::onTriggerByIntentClick,
        )

        OptionPageButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.key_map_options_create_shortcut_title),
            text = if (state.isLauncherShortcutButtonEnabled) {
                stringResource(R.string.key_map_options_create_shortcut_summary)
            } else {
                stringResource(R.string.key_map_options_create_shortcut_summary_unsupported)
            },
            icon = Icons.Outlined.AddHome,
            enabled = state.triggerFromOtherApps && state.isLauncherShortcutButtonEnabled,
            onClick = callback::onCreateShortcutClick,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun KeyMapUidRow(modifier: Modifier = Modifier, keyMapUid: String) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.key_map_options_key_map_id),
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = keyMapUid,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val clipboardLabel = stringResource(R.string.clipboard_label_keymap_uid)

        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText(clipboardLabel, keyMapUid)),
                    )
                }
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = stringResource(
                    R.string.flag_trigger_from_other_apps_copy_uid,
                ),
            )
        }
    }
}

interface KeyMapOptionsCallback {
    fun onLongPressDelayChanged(delay: Int) = run { }
    fun onDoublePressDelayChanged(delay: Int) = run { }
    fun onSequenceTriggerTimeoutChanged(timeout: Int) = run { }
    fun onVibrateDurationChanged(duration: Int) = run { }
    fun onVibrateChanged(checked: Boolean) = run { }
    fun onLongPressDoubleVibrationChanged(checked: Boolean) = run { }
    fun onShowToastChanged(checked: Boolean) = run { }
    fun onTriggerFromOtherAppsChanged(checked: Boolean) = run {}
    fun onCreateShortcutClick() = run { }
    fun onOpenExpertModeSettings() = run {}
    fun onTriggerByIntentClick() = run {}
    fun onEnableByIntentClick() = run {}
}

@Preview(heightDp = 1300)
@Composable
private fun Preview() {
    KeyMapperTheme {
        Surface {
            KeyMapOptionsScreen(
                state = State.Data(
                    KeyMapOptionsState(
                        showLongPressDelay = true,
                        longPressDelay = 300,
                        defaultLongPressDelay = 400,

                        showDoublePressDelay = true,
                        doublePressDelay = 100,
                        defaultDoublePressDelay = 100,

                        showSequenceTriggerTimeout = true,
                        sequenceTriggerTimeout = 1000,
                        defaultSequenceTriggerTimeout = 1000,

                        showVibrateDuration = true,
                        vibrateDuration = 100,
                        defaultVibrateDuration = 100,

                        showVibrate = true,
                        vibrate = true,

                        showLongPressDoubleVibration = true,
                        longPressDoubleVibration = false,

                        triggerFromOtherApps = true,
                        keyMapUid = "beea7ef5-e33e-4bd3-9987-9002e5035f23",
                        isLauncherShortcutButtonEnabled = false,

                        showToast = true,
                        showScreenOffTip = true,
                        isExpertModeStarted = false,
                    ),
                ),
                callback = object : KeyMapOptionsCallback {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEvdevTrigger() {
    KeyMapperTheme {
        Surface {
            KeyMapOptionsScreen(
                state = State.Data(
                    KeyMapOptionsState(
                        showLongPressDelay = false,
                        longPressDelay = 300,
                        defaultLongPressDelay = 400,

                        showDoublePressDelay = false,
                        doublePressDelay = 100,
                        defaultDoublePressDelay = 100,

                        showSequenceTriggerTimeout = false,
                        sequenceTriggerTimeout = 1000,
                        defaultSequenceTriggerTimeout = 1000,

                        showVibrateDuration = false,
                        vibrateDuration = 100,
                        defaultVibrateDuration = 100,

                        showVibrate = true,
                        vibrate = false,

                        showLongPressDoubleVibration = false,
                        longPressDoubleVibration = false,

                        triggerFromOtherApps = false,
                        keyMapUid = "beea7ef5-e33e-4bd3-9987-9002e5035f23",
                        isLauncherShortcutButtonEnabled = true,

                        showToast = false,
                        showScreenOffTip = false,
                        isExpertModeStarted = true,
                    ),
                ),
                callback = object : KeyMapOptionsCallback {},
            )
        }
    }
}
