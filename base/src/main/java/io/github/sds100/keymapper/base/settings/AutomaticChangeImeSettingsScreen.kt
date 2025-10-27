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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import io.github.sds100.keymapper.base.utils.ui.compose.OptionPageButton
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.SwitchPreferenceCompose

@Composable
fun AutomaticChangeImeSettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val state by viewModel.automaticChangeImeSettingsState.collectAsStateWithLifecycle()
    val snackbarHostState = SnackbarHostState()

    AutomaticChangeImeSettingsScreen(
        modifier,
        onBackClick = viewModel::onBackClick,
        snackbarHostState = snackbarHostState,
    ) {
        Content(
            state = state,
            onShowToastWhenAutoChangingImeToggled =
            viewModel::onShowToastWhenAutoChangingImeToggled,
            onChangeImeOnInputFocusToggled = viewModel::onChangeImeOnInputFocusToggled,
            onChangeImeOnDeviceConnectToggled = viewModel::onChangeImeOnDeviceConnectToggled,
            onDevicesThatChangeImeClick = viewModel::onDevicesThatChangeImeClick,
            onToggleKeyboardOnToggleKeymapsToggled =
            viewModel::onToggleKeyboardOnToggleKeymapsToggled,
            onShowToggleKeyboardNotificationClick =
            viewModel::onShowToggleKeyboardNotificationClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomaticChangeImeSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_pref_automatically_change_ime)) },
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
    state: AutomaticChangeImeSettingsState,
    onShowToastWhenAutoChangingImeToggled: (Boolean) -> Unit = { },
    onChangeImeOnInputFocusToggled: (Boolean) -> Unit = { },
    onChangeImeOnDeviceConnectToggled: (Boolean) -> Unit = { },
    onDevicesThatChangeImeClick: () -> Unit = { },
    onToggleKeyboardOnToggleKeymapsToggled: (Boolean) -> Unit = { },
    onShowToggleKeyboardNotificationClick: () -> Unit = { },
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_auto_change_ime_on_input_focus),
            text = stringResource(R.string.summary_pref_auto_change_ime_on_input_focus),
            icon = Icons.Rounded.SwapHoriz,
            isChecked = state.changeImeOnInputFocus,
            onCheckedChange = onChangeImeOnInputFocusToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_auto_change_ime_on_connection),
            text = stringResource(R.string.summary_pref_auto_change_ime_on_connection),
            icon = Icons.Rounded.SwapHoriz,
            isChecked = state.changeImeOnDeviceConnect,
            onCheckedChange = onChangeImeOnDeviceConnectToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionPageButton(
            title = stringResource(R.string.title_pref_automatically_change_ime_choose_devices),
            text = stringResource(R.string.summary_pref_automatically_change_ime_choose_devices),
            icon = Icons.Rounded.Devices,
            onClick = onDevicesThatChangeImeClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_toggle_keyboard_on_toggle_keymaps),
            text = stringResource(R.string.summary_pref_toggle_keyboard_on_toggle_keymaps),
            icon = Icons.Rounded.SwapHoriz,
            isChecked = state.toggleKeyboardOnToggleKeymaps,
            onCheckedChange = onToggleKeyboardOnToggleKeymapsToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Notifications,
            text = stringResource(R.string.settings_section_notifications),
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_show_toast_when_auto_changing_ime),
            text = stringResource(R.string.summary_pref_show_toast_when_auto_changing_ime),
            icon = Icons.Rounded.Notifications,
            isChecked = state.showToastWhenAutoChangingIme,
            onCheckedChange = onShowToastWhenAutoChangingImeToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OptionPageButton(
                title = stringResource(R.string.title_pref_show_toggle_keyboard_notification),
                text = stringResource(R.string.summary_pref_show_toggle_keyboard_notification),
                icon = Icons.Rounded.Notifications,
                onClick = onShowToggleKeyboardNotificationClick,
            )
        } else {
            // For older Android versions, this would be a switch but since we're targeting newer versions
            // we'll just show the notification settings button
            OptionPageButton(
                title = stringResource(R.string.title_pref_show_toggle_keyboard_notification),
                text = stringResource(R.string.summary_pref_show_toggle_keyboard_notification),
                icon = Icons.Rounded.Notifications,
                onClick = onShowToggleKeyboardNotificationClick,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        AutomaticChangeImeSettingsScreen(modifier = Modifier.fillMaxSize(), onBackClick = {}) {
            Content(
                state = AutomaticChangeImeSettingsState(),
            )
        }
    }
}
