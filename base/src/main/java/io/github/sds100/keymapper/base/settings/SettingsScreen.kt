package io.github.sds100.keymapper.base.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
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
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.backup.BackupUtils
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.base.utils.ui.compose.OptionPageButton
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.SwitchPreferenceCompose
import io.github.sds100.keymapper.base.utils.ui.compose.icons.FolderManaged
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.ProModeIcon
import io.github.sds100.keymapper.base.utils.ui.compose.icons.WandStars
import io.github.sds100.keymapper.common.utils.BuildUtils
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.system.files.FileUtils
import kotlinx.coroutines.launch

private val isProModeSupported = Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val state by viewModel.mainScreenState.collectAsStateWithLifecycle()
    val snackbarHostState = SnackbarHostState()
    var showAutomaticBackupDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val automaticBackupLocationChooser =
        rememberLauncherForActivityResult(CreateDocument(FileUtils.MIME_TYPE_ZIP)) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            viewModel.setAutomaticBackupLocation(uri.toString())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }

    if (showAutomaticBackupDialog) {
        val activityNotFoundText =
            stringResource(R.string.dialog_message_no_app_found_to_create_file)

        AlertDialog(
            onDismissRequest = { showAutomaticBackupDialog = false },
            title = { Text(stringResource(R.string.dialog_title_change_location_or_disable)) },
            text = { Text(stringResource(R.string.dialog_message_change_location_or_disable)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAutomaticBackupDialog = false

                        try {
                            automaticBackupLocationChooser.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)
                        } catch (e: ActivityNotFoundException) {
                            scope.launch {
                                snackbarHostState.showSnackbar(activityNotFoundText)
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.pos_change_location))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAutomaticBackupDialog = false
                        viewModel.disableAutomaticBackup()
                    },
                ) {
                    Text(stringResource(R.string.neg_turn_off))
                }
            },
        )
    }

    SettingsScreen(
        modifier,
        onBackClick = viewModel::onBackClick,
        viewModel::onResetAllSettingsClick,
        snackbarHostState = snackbarHostState,
    ) {
        Content(
            state = state,
            onThemeSelected = viewModel::onThemeSelected,
            onPauseResumeNotificationClick = viewModel::onPauseResumeNotificationClick,
            onDefaultOptionsClick = viewModel::onDefaultOptionsClick,
            onProModeClick = {
                if (isProModeSupported) {
                    viewModel.onProModeClick()
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.error_sdk_version_too_low,
                                BuildUtils.getSdkVersionName(Constants.SYSTEM_BRIDGE_MIN_API),
                            ),
                        )
                    }
                }
            },
            onAutomaticChangeImeClick = viewModel::onAutomaticChangeImeClick,
            onForceVibrateToggled = viewModel::onForceVibrateToggled,
            onLoggingToggled = viewModel::onLoggingToggled,
            onViewLogClick = viewModel::onViewLogClick,
            onHideHomeScreenAlertsToggled = viewModel::onHideHomeScreenAlertsToggled,
            onShowDeviceDescriptorsToggled = viewModel::onShowDeviceDescriptorsToggled,
            onAutomaticBackupClick = {
                if (state.autoBackupLocation.isNullOrBlank()) {
                    automaticBackupLocationChooser.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)
                } else {
                    showAutomaticBackupDialog = true
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onResetClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
                actions = {
                    OutlinedButton(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onResetClick,
                    ) {
                        Text(stringResource(R.string.settings_reset_app_bar_button))
                    }
                },
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
    state: MainSettingsState,
    onThemeSelected: (Theme) -> Unit = { },
    onPauseResumeNotificationClick: () -> Unit = { },
    onDefaultOptionsClick: () -> Unit = { },
    onAutomaticBackupClick: () -> Unit = { },
    onProModeClick: () -> Unit = { },
    onAutomaticChangeImeClick: () -> Unit = { },
    onForceVibrateToggled: (Boolean) -> Unit = { },
    onLoggingToggled: (Boolean) -> Unit = { },
    onViewLogClick: () -> Unit = { },
    onHideHomeScreenAlertsToggled: (Boolean) -> Unit = { },
    onShowDeviceDescriptorsToggled: (Boolean) -> Unit = { },
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            modifier = Modifier.fillMaxWidth(),
            icon = KeyMapperIcons.WandStars,
            text = stringResource(R.string.settings_section_customize_experience_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.title_pref_dark_theme),
            style = MaterialTheme.typography.bodyLarge,
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

        OptionPageButton(
            title = stringResource(R.string.title_pref_show_toggle_keymaps_notification),
            text = stringResource(R.string.summary_pref_show_toggle_keymaps_notification),
            icon = Icons.Rounded.PlayCircleOutline,
            onClick = onPauseResumeNotificationClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_hide_home_screen_alerts),
            text = stringResource(R.string.summary_pref_hide_home_screen_alerts),
            icon = Icons.Rounded.VisibilityOff,
            isChecked = state.hideHomeScreenAlerts,
            onCheckedChange = onHideHomeScreenAlertsToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Gamepad,
            text = stringResource(R.string.settings_section_key_maps_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionPageButton(
            title = stringResource(R.string.title_pref_default_options),
            text = stringResource(R.string.summary_pref_default_options),
            icon = Icons.Rounded.Tune,
            onClick = onDefaultOptionsClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_force_vibrate),
            text = stringResource(R.string.summary_pref_force_vibrate),
            icon = Icons.Rounded.Vibration,
            isChecked = state.forceVibrate,
            onCheckedChange = onForceVibrateToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_show_device_descriptors),
            text = stringResource(R.string.summary_pref_show_device_descriptors),
            icon = Icons.Rounded.Devices,
            isChecked = state.showDeviceDescriptors,
            onCheckedChange = onShowDeviceDescriptorsToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            modifier = Modifier.fillMaxWidth(),
            icon = KeyMapperIcons.FolderManaged,
            text = stringResource(R.string.settings_section_data_management_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionPageButton(
            title = if (state.autoBackupLocation == null) {
                stringResource(R.string.title_pref_automatic_backup_location_disabled)
            } else {
                stringResource(R.string.title_pref_automatic_backup_location_enabled)
            },
            text = state.autoBackupLocation
                ?: stringResource(R.string.summary_pref_automatic_backup_location_disabled),
            icon = Icons.Rounded.Tune,
            onClick = onAutomaticBackupClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Construction,
            text = stringResource(R.string.settings_section_power_user_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionPageButton(
            title = if (isProModeSupported) {
                stringResource(R.string.title_pref_pro_mode)
            } else {
                stringResource(R.string.title_pref_pro_mode)
            },
            text = if (isProModeSupported) {
                stringResource(R.string.summary_pref_pro_mode)
            } else {
                stringResource(
                    R.string.error_sdk_version_too_low,
                    BuildUtils.getSdkVersionName(Constants.SYSTEM_BRIDGE_MIN_API),
                )
            },
            icon = KeyMapperIcons.ProModeIcon,
            onClick = onProModeClick,
        )

        OptionPageButton(
            title = stringResource(R.string.title_pref_automatically_change_ime),
            text = stringResource(R.string.summary_pref_automatically_change_ime),
            icon = Icons.Rounded.Keyboard,
            onClick = onAutomaticChangeImeClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionsHeaderRow(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Code,
            text = stringResource(R.string.settings_section_debugging_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceCompose(
            title = stringResource(R.string.title_pref_toggle_logging),
            text = stringResource(R.string.summary_pref_toggle_logging),
            icon = Icons.Outlined.BugReport,
            isChecked = state.loggingEnabled,
            onCheckedChange = onLoggingToggled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OptionPageButton(
            title = stringResource(R.string.title_pref_view_and_share_log),
            text = stringResource(R.string.summary_pref_view_and_share_log),
            icon = Icons.Outlined.FindInPage,
            onClick = onViewLogClick,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        SettingsScreen(modifier = Modifier.fillMaxSize(), onBackClick = {}) {
            Content(
                state = MainSettingsState(),
            )
        }
    }
}
