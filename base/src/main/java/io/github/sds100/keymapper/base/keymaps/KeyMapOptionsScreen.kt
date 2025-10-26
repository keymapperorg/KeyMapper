package io.github.sds100.keymapper.base.keymaps

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.SliderMaximums
import io.github.sds100.keymapper.base.utils.ui.SliderMinimums
import io.github.sds100.keymapper.base.utils.ui.SliderStepSizes
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import io.github.sds100.keymapper.base.utils.ui.compose.openUriSafe
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
            .fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        TriggerFromOtherAppsSection(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            isChecked = state.triggerFromOtherApps,
            onCheckedChange = callback::onTriggerFromOtherAppsChanged,
            isLauncherShortcutEnabled = state.isLauncherShortcutButtonEnabled,
            keyMapUid = state.keyMapUid,
            onCreateShortcutClick = callback::onCreateShortcutClick,
        )

        Spacer(Modifier.height(8.dp))

        CheckBoxText(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.flag_show_toast),
            isChecked = state.showToast,
            onCheckedChange = callback::onShowToastChanged,
        )
        Spacer(Modifier.height(8.dp))

        if (state.showVibrate) {
            CheckBoxText(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.flag_vibrate),
                isChecked = state.vibrate,
                onCheckedChange = callback::onVibrateChanged,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.showVibrateDuration) {
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.extra_label_vibration_duration),
                defaultValue = state.defaultVibrateDuration.toFloat(),
                value = state.vibrateDuration.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onVibrateDurationChanged(it.toInt()) },
                valueRange =
                SliderMinimums.VIBRATION_DURATION.toFloat()..SliderMaximums.VIBRATION_DURATION.toFloat(),
                stepSize = SliderStepSizes.VIBRATION_DURATION,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.showLongPressDoubleVibration) {
            CheckBoxText(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.flag_long_press_double_vibration),
                isChecked = state.longPressDoubleVibration,
                onCheckedChange = callback::onLongPressDoubleVibrationChanged,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.showLongPressDelay) {
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.extra_label_long_press_delay_timeout),
                defaultValue = state.defaultLongPressDelay.toFloat(),
                value = state.longPressDelay.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onLongPressDelayChanged(it.toInt()) },
                valueRange =
                SliderMinimums.TRIGGER_LONG_PRESS_DELAY.toFloat()..SliderMaximums.TRIGGER_LONG_PRESS_DELAY.toFloat(),
                stepSize = SliderStepSizes.TRIGGER_LONG_PRESS_DELAY,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.showDoublePressDelay) {
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.extra_label_double_press_delay_timeout),
                defaultValue = state.defaultDoublePressDelay.toFloat(),
                value = state.doublePressDelay.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onDoublePressDelayChanged(it.toInt()) },
                valueRange =
                SliderMinimums.TRIGGER_DOUBLE_PRESS_DELAY.toFloat()..SliderMaximums.TRIGGER_DOUBLE_PRESS_DELAY.toFloat(),
                stepSize = SliderStepSizes.TRIGGER_DOUBLE_PRESS_DELAY,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.showSequenceTriggerTimeout) {
            SliderOptionText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.extra_label_sequence_trigger_timeout),
                defaultValue = state.defaultSequenceTriggerTimeout.toFloat(),
                value = state.sequenceTriggerTimeout.toFloat(),
                valueText = { "${it.toInt()} ms" },
                onValueChange = { callback.onSequenceTriggerTimeoutChanged(it.toInt()) },
                valueRange =
                SliderMinimums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT.toFloat()..SliderMaximums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT.toFloat(),
                stepSize = SliderStepSizes.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT,
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TriggerFromOtherAppsSection(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onCreateShortcutClick: () -> Unit,
    isLauncherShortcutEnabled: Boolean,
    keyMapUid: String,
) {
    Column(modifier) {
        Row {
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onCheckedChange(!isChecked) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isChecked,
                        // This is null so tapping on the checkbox highlights the whole row.
                        onCheckedChange = null,
                    )

                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),

                        text = stringResource(R.string.flag_trigger_from_other_apps),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        AnimatedVisibility(isChecked) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .weight(1f),
                        text = keyMapUid,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    val clipboard = LocalClipboard.current
                    val scope = rememberCoroutineScope()
                    val clipboardLabel = stringResource(R.string.clipboard_label_keymap_uid)

                    IconButton(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(36.dp),
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

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = onCreateShortcutClick,
                    enabled = isLauncherShortcutEnabled,
                ) {
                    val text = if (isLauncherShortcutEnabled) {
                        stringResource(R.string.button_create_keymap_shortcut_in_launcher_enabled)
                    } else {
                        stringResource(R.string.button_create_keymap_shortcut_in_launcher_disabled)
                    }

                    Text(text = text)
                }

                val uriHandler = LocalUriHandler.current
                val ctx = LocalContext.current
                val intentGuideUrl = stringResource(R.string.url_trigger_by_intent_guide)

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = {
                        uriHandler.openUriSafe(ctx, intentGuideUrl)
                    },
                ) {
                    Text(
                        text = stringResource(
                            R.string.button_open_trigger_keymap_from_intent_guide,
                        ),
                    )
                }
            }
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
}

@Preview
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
                        keyMapUid = "00000-00000-00000-0000000000000000000000000000000000",
                        isLauncherShortcutButtonEnabled = false,

                        showToast = true,
                    ),
                ),
                callback = object : KeyMapOptionsCallback {},
            )
        }
    }
}
