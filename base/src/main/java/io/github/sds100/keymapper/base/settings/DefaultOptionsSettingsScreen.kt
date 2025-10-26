package io.github.sds100.keymapper.base.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.SliderMaximums
import io.github.sds100.keymapper.base.utils.ui.SliderMinimums
import io.github.sds100.keymapper.base.utils.ui.SliderStepSizes
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText

@Composable
fun DefaultOptionsSettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val state by viewModel.defaultSettingsScreenState.collectAsStateWithLifecycle()

    DefaultOptionsSettingsScreen(
        modifier,
        onBackClick = viewModel::onBackClick,
    ) {
        Content(
            state = state,
            callback = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultOptionsSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_pref_default_options)) },
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_go_back),
                    )
                }
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = innerPadding.calculateStartPadding(layoutDirection)
        val endPadding = innerPadding.calculateEndPadding(layoutDirection)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = startPadding,
                    end = endPadding,
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: DefaultSettingsState,
    callback: DefaultOptionsSettingsCallback = object : DefaultOptionsSettingsCallback {},
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Long press delay
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.title_pref_long_press_delay),
            defaultValue = state.defaultLongPressDelay.toFloat(),
            value = state.longPressDelay.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { callback.onLongPressDelayChanged(it.toInt()) },
            valueRange =
            SliderMinimums.TRIGGER_LONG_PRESS_DELAY.toFloat()..SliderMaximums.TRIGGER_LONG_PRESS_DELAY.toFloat(),
            stepSize = SliderStepSizes.TRIGGER_LONG_PRESS_DELAY,
        )
        Spacer(Modifier.height(8.dp))

        // Double press delay
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.title_pref_double_press_delay),
            defaultValue = state.defaultDoublePressDelay.toFloat(),
            value = state.doublePressDelay.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { callback.onDoublePressDelayChanged(it.toInt()) },
            valueRange =
            SliderMinimums.TRIGGER_DOUBLE_PRESS_DELAY.toFloat()..SliderMaximums.TRIGGER_DOUBLE_PRESS_DELAY.toFloat(),
            stepSize = SliderStepSizes.TRIGGER_DOUBLE_PRESS_DELAY,
        )
        Spacer(Modifier.height(8.dp))

        // Vibrate duration
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.title_pref_vibration_duration),
            defaultValue = state.defaultVibrateDuration.toFloat(),
            value = state.vibrateDuration.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { callback.onVibrateDurationChanged(it.toInt()) },
            valueRange =
            SliderMinimums.VIBRATION_DURATION.toFloat()..SliderMaximums.VIBRATION_DURATION.toFloat(),
            stepSize = SliderStepSizes.VIBRATION_DURATION,
        )
        Spacer(Modifier.height(8.dp))

        // Repeat delay
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.title_pref_repeat_delay),
            defaultValue = state.defaultRepeatDelay.toFloat(),
            value = state.repeatDelay.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { callback.onRepeatDelayChanged(it.toInt()) },
            valueRange =
            SliderMinimums.ACTION_REPEAT_DELAY.toFloat()..SliderMaximums.ACTION_REPEAT_DELAY.toFloat(),
            stepSize = SliderStepSizes.ACTION_REPEAT_DELAY,
        )
        Spacer(Modifier.height(8.dp))

        // Repeat rate
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.title_pref_repeat_rate),
            defaultValue = state.defaultRepeatRate.toFloat(),
            value = state.repeatRate.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { callback.onRepeatRateChanged(it.toInt()) },
            valueRange =
            SliderMinimums.ACTION_REPEAT_RATE.toFloat()..SliderMaximums.ACTION_REPEAT_RATE.toFloat(),
            stepSize = SliderStepSizes.ACTION_REPEAT_RATE,
        )
        Spacer(Modifier.height(8.dp))

        // Sequence trigger timeout
        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.title_pref_sequence_trigger_timeout),
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
}

interface DefaultOptionsSettingsCallback {
    fun onLongPressDelayChanged(delay: Int) = run { }
    fun onDoublePressDelayChanged(delay: Int) = run { }
    fun onVibrateDurationChanged(duration: Int) = run { }
    fun onRepeatDelayChanged(delay: Int) = run { }
    fun onRepeatRateChanged(rate: Int) = run { }
    fun onSequenceTriggerTimeoutChanged(timeout: Int) = run { }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        DefaultOptionsSettingsScreen(modifier = Modifier.fillMaxSize(), onBackClick = {}) {
            Content(
                state = DefaultSettingsState(),
            )
        }
    }
}
