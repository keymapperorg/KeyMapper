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
            onLongPressDelayChanged = viewModel::onLongPressDelayChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultOptionsSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    content: @Composable () -> Unit
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
    modifier: Modifier = Modifier, state: DefaultSettingsState,
    onLongPressDelayChanged: (Int) -> Unit = { },
) {
    Column(
        modifier.verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.extra_label_long_press_delay_timeout),
            defaultValue = state.defaultLongPressDelay.toFloat(),
            value = state.longPressDelay.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { onLongPressDelayChanged(it.toInt()) },
            valueRange = SliderMinimums.TRIGGER_LONG_PRESS_DELAY.toFloat()..SliderMaximums.TRIGGER_LONG_PRESS_DELAY.toFloat(),
            stepSize = SliderStepSizes.TRIGGER_LONG_PRESS_DELAY,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        DefaultOptionsSettingsScreen(modifier = Modifier.fillMaxSize(), onBackClick = {}) {
            Content(
                state = DefaultSettingsState()
            )
        }
    }
}