package io.github.sds100.keymapper.base.settings

import android.os.Build
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
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.base.utils.ui.compose.OptionPageButton
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.WandStars

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val state by viewModel.mainScreenState.collectAsStateWithLifecycle()

    SettingsScreen(
        modifier,
        onBackClick = viewModel::onBackClick,
        viewModel::onResetAllSettingsClick
    ) {
        Content(
            state = state,
            onThemeSelected = viewModel::onThemeSelected,
            onPauseResumeNotificationClick = viewModel::onPauseResumeNotificationClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onResetClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
                actions = {
                    OutlinedButton(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onResetClick
                    ) {
                        Text(stringResource(R.string.settings_reset_app_bar_button))
                    }
                }
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
    state: SettingsState,
    onThemeSelected: (Theme) -> Unit = { },
    onPauseResumeNotificationClick: () -> Unit = { },
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            modifier = Modifier.fillMaxWidth(),
            icon = KeyMapperIcons.WandStars,
            text = stringResource(R.string.settings_section_customize_experience_title)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.title_pref_dark_theme),
            style = MaterialTheme.typography.bodyLarge
        )

        val buttonStates: List<Pair<Theme, String>> = listOf(
            Theme.AUTO to stringResource(R.string.theme_system),
            Theme.LIGHT to stringResource(R.string.theme_light),
            Theme.DARK to stringResource(R.string.theme_dark),
        )

        Spacer(modifier = Modifier.height(8.dp))

        KeyMapperSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
            buttonStates,
            state.theme,
            onStateSelected = onThemeSelected,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OptionPageButton(
                title = stringResource(R.string.title_pref_show_toggle_keymaps_notification),
                text = stringResource(R.string.summary_pref_show_toggle_keymaps_notification),
                icon = Icons.Rounded.PlayCircleOutline,
                onClick = onPauseResumeNotificationClick
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        SettingsScreen(modifier = Modifier.fillMaxSize(), onBackClick = {}) {
            Content(
                state = SettingsState()
            )
        }
    }
}