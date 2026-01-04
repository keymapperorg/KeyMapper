package io.github.sds100.keymapper.base.expertmode

import android.content.ClipData
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.SwitchPreferenceCompose
import io.github.sds100.keymapper.base.utils.ui.compose.icons.FakeShizuku
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcon
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.launch

@Composable
fun ExpertModeScreen(modifier: Modifier = Modifier, viewModel: ExpertModeViewModel) {
    val expertModeWarningState by viewModel.warningState.collectAsStateWithLifecycle()
    val expertModeState by viewModel.state.collectAsStateWithLifecycle()

    ExpertModeScreen(
        modifier = modifier,
        onBackClick = viewModel::onBackClick,
        onHelpClick = { viewModel.showInfoCard() },
        showHelpIcon = !viewModel.showInfoCard,
    ) {
        Content(
            warningState = expertModeWarningState,
            setupState = expertModeState,
            showInfoCard = viewModel.showInfoCard,
            onInfoCardDismiss = { viewModel.hideInfoCard() },
            onWarningButtonClick = viewModel::onWarningButtonClick,
            onStopServiceClick = viewModel::onStopServiceClick,
            onShizukuButtonClick = viewModel::onShizukuButtonClick,
            onRootButtonClick = viewModel::onRootButtonClick,
            onSetupWithKeyMapperClick = viewModel::onSetupWithKeyMapperClick,
            onRequestNotificationPermissionClick = viewModel::onRequestNotificationPermissionClick,
            onAutoStartAtBootToggled = { viewModel.onAutoStartBootToggled() },
            onLaunchDeveloperOptionsClick = viewModel::onLaunchDeveloperOptionsClick,
            onGetShellStartCommandClick = viewModel::onGetShellStartCommandClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpertModeScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    showHelpIcon: Boolean = false,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expert_mode_app_bar_title)) },
                actions = {
                    AnimatedVisibility(
                        visible = showHelpIcon,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        IconButton(onClick = onHelpClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = stringResource(
                                    R.string.expert_mode_info_card_show_content_description,
                                ),
                            )
                        }
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
    warningState: ExpertModeWarningState,
    setupState: State<ExpertModeState>,
    showInfoCard: Boolean,
    onInfoCardDismiss: () -> Unit = {},
    onWarningButtonClick: () -> Unit = {},
    onShizukuButtonClick: () -> Unit = {},
    onStopServiceClick: () -> Unit = {},
    onRootButtonClick: () -> Unit = {},
    onSetupWithKeyMapperClick: () -> Unit = {},
    onRequestNotificationPermissionClick: () -> Unit = {},
    onAutoStartAtBootToggled: () -> Unit = {},
    onLaunchDeveloperOptionsClick: () -> Unit = {},
    onGetShellStartCommandClick: () -> Unit = {},
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        AnimatedVisibility(
            visible = showInfoCard,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            ExpertModeInfoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                onDismiss = onInfoCardDismiss,
            )
        }

        if (showInfoCard) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        WarningCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            state = warningState,
            onButtonClick = onWarningButtonClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (warningState is ExpertModeWarningState.Understood) {
            when (setupState) {
                is State.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                is State.Data -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxWidth(),
                        state = setupState.data,
                        onShizukuButtonClick = onShizukuButtonClick,
                        onStopServiceClick = onStopServiceClick,
                        onRootButtonClick = onRootButtonClick,
                        onSetupWithKeyMapperClick = onSetupWithKeyMapperClick,
                        onRequestNotificationPermissionClick = onRequestNotificationPermissionClick,
                        onAutoStartAtBootToggled = onAutoStartAtBootToggled,
                        onLaunchDeveloperOptionsClick = onLaunchDeveloperOptionsClick,
                        onGetShellStartCommandClick = onGetShellStartCommandClick,
                    )
                }
            }
        } else {
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.expert_mode_settings_unavailable_text),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier,
    state: ExpertModeState,
    onRootButtonClick: () -> Unit = {},
    onShizukuButtonClick: () -> Unit,
    onStopServiceClick: () -> Unit,
    onSetupWithKeyMapperClick: () -> Unit,
    onRequestNotificationPermissionClick: () -> Unit = {},
    onAutoStartAtBootToggled: () -> Unit = {},
    onLaunchDeveloperOptionsClick: () -> Unit = {},
    onGetShellStartCommandClick: () -> Unit = {},
) {
    Column(modifier) {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.Checklist,
            text = stringResource(R.string.expert_mode_set_up_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show notification permission warning if permission not granted
        if (state is ExpertModeState.Stopped && !state.isNotificationPermissionGranted) {
            val text =
                stringResource(
                    R.string.expert_mode_setup_wizard_enable_notification_permission_description,
                )

            SetupCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
                title = stringResource(
                    R.string.expert_mode_setup_wizard_enable_notification_permission_title,
                ),
                content = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                buttonText = stringResource(
                    R.string.expert_mode_setup_wizard_enable_notification_permission_button,
                ),
                onButtonClick = onRequestNotificationPermissionClick,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when (state) {
            is ExpertModeState.Started -> {
                ExpertModeStartedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    onStopClick = onStopServiceClick,
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (!state.isDefaultUsbModeCompatible) {
                    IncompatibleUsbModeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )

                    Spacer(Modifier.height(8.dp))
                }

                // Only show auto-start options and warnings when Expert Mode is started
                // Show USB debugging security settings warning if disabled
                if (state.isAdbInputSecurityEnabled == false) {
                    UsbDebuggingSecuritySettingsCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        onLaunchDeveloperOptionsClick = onLaunchDeveloperOptionsClick,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                SwitchPreferenceCompose(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = stringResource(R.string.title_pref_expert_mode_auto_start),
                    text = if (state.autoStartBootEnabled) {
                        stringResource(R.string.summary_pref_expert_mode_auto_start)
                    } else {
                        stringResource(
                            R.string.summary_pref_expert_mode_auto_start_disabled,
                        )
                    },
                    icon = Icons.Rounded.RestartAlt,
                    isChecked = state.autoStartBootChecked,
                    onCheckedChange = { onAutoStartAtBootToggled() },
                    isEnabled = state.autoStartBootEnabled,

                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            is ExpertModeState.Stopped -> {
                if (state.isRootGranted) {
                    SetupCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = LocalCustomColorsPalette.current.magiskTeal,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Numbers,
                                contentDescription = null,
                                tint = LocalCustomColorsPalette.current.magiskTeal,
                            )
                        },
                        title = stringResource(R.string.expert_mode_root_detected_title),
                        content = {
                            Text(
                                text = stringResource(R.string.expert_mode_root_detected_text),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        buttonText = stringResource(
                            R.string.expert_mode_root_detected_button_start_service,
                        ),
                        onButtonClick = onRootButtonClick,
                        enabled = state.isNotificationPermissionGranted,
                        isLoading = state.isStarting,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                val shizukuButtonText: String? = when (state.shizukuSetupState) {
                    ShizukuSetupState.INSTALLED -> stringResource(
                        R.string.expert_mode_shizuku_detected_button_start,
                    )

                    ShizukuSetupState.STARTED -> stringResource(
                        R.string.expert_mode_shizuku_detected_button_request_permission,
                    )

                    ShizukuSetupState.PERMISSION_GRANTED -> stringResource(
                        R.string.expert_mode_shizuku_detected_button_start_service,
                    )

                    ShizukuSetupState.NOT_FOUND -> null
                }

                if (shizukuButtonText != null) {
                    SetupCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = LocalCustomColorsPalette.current.shizukuBlue,
                        icon = {
                            Image(
                                imageVector = KeyMapperIcons.FakeShizuku,
                                contentDescription = null,
                            )
                        },
                        title = stringResource(R.string.expert_mode_shizuku_detected_title),
                        content = {
                            Text(
                                text = stringResource(R.string.expert_mode_shizuku_detected_text),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        buttonText = shizukuButtonText,
                        onButtonClick = onShizukuButtonClick,
                        enabled = state.isNotificationPermissionGranted,
                        isLoading = state.isStarting,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val setupKeyMapperText: String = when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> stringResource(
                        R.string.expert_mode_set_up_with_key_mapper_button_incompatible,
                    )

                    else -> stringResource(R.string.expert_mode_set_up_with_key_mapper_button)
                }

                SetupCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    icon = {
                        Image(
                            modifier = Modifier.padding(2.dp),
                            imageVector = KeyMapperIcons.KeyMapperIcon,
                            contentDescription = null,
                        )
                    },
                    title = stringResource(R.string.expert_mode_set_up_with_key_mapper_title),
                    content = {},
                    buttonText = setupKeyMapperText,
                    onButtonClick = onSetupWithKeyMapperClick,
                    enabled =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                            state.isNotificationPermissionGranted,
                    isLoading = state.isStarting,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ShellStartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shellStartCommandState = state.shellStartCommandState,
                    onGetShellStartCommandClick = onGetShellStartCommandClick,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun IncompatibleUsbModeCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    SetupCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Usb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        title = stringResource(
            R.string.expert_mode_setup_wizard_change_default_usb_configuration_title,
        ),
        content = {
            Text(
                text = stringResource(
                    R.string.expert_mode_setup_wizard_change_default_usb_configuration_description,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        buttonText = stringResource(
            R.string.button_fix,
        ),
        onButtonClick = {
            // Go to developer options and highlight the "Default USB configuration" option
            SettingsUtils.launchSettingsScreen(
                ctx,
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                "default_usb_configuration",
            )
        },
    )
}

@Composable
private fun UsbDebuggingSecuritySettingsCard(
    modifier: Modifier = Modifier,
    onLaunchDeveloperOptionsClick: () -> Unit = {},
) {
    SetupCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        },
        title = stringResource(
            R.string.expert_mode_usb_debugging_security_settings_title,
        ),
        content = {
            Text(
                text = stringResource(
                    R.string.expert_mode_usb_debugging_security_settings_description,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        buttonText = stringResource(
            R.string.expert_mode_usb_debugging_security_settings_button,
        ),
        onButtonClick = onLaunchDeveloperOptionsClick,
    )
}

@Composable
private fun WarningCard(
    modifier: Modifier = Modifier,
    state: ExpertModeWarningState,
    onButtonClick: () -> Unit = {},
) {
    val borderStroke = if (state is ExpertModeWarningState.Understood) {
        CardDefaults.outlinedCardBorder()
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    }

    OutlinedCard(
        modifier = modifier,
        border = borderStroke,
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.expert_mode_warning_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.expert_mode_warning_text),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp),
            onClick = onButtonClick,
            enabled = state is ExpertModeWarningState.Idle,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            if (state is ExpertModeWarningState.Understood) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null)

                Spacer(modifier = Modifier.width(8.dp))
            }

            val text = when (state) {
                is ExpertModeWarningState.CountingDown -> stringResource(
                    R.string.expert_mode_warning_understand_button_countdown,
                    state.seconds,
                )

                ExpertModeWarningState.Idle -> stringResource(
                    R.string.expert_mode_warning_understand_button_not_completed,
                )

                ExpertModeWarningState.Understood -> stringResource(
                    R.string.expert_mode_warning_understand_button_completed,
                )
            }

            Text(text)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExpertModeStartedCard(modifier: Modifier = Modifier, onStopClick: () -> Unit = {}) {
    OutlinedCard(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = LocalCustomColorsPalette.current.green,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                text = stringResource(R.string.expert_mode_service_started),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.width(16.dp))

            TextButton(
                onClick = onStopClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.expert_mode_stop_service_button))
            }

            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun SetupCard(
    modifier: Modifier = Modifier,
    color: Color,
    icon: @Composable () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit = {},
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    OutlinedCard(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(Modifier.size(24.dp)) {
                icon()
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp),
            onClick = onButtonClick,
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = color,
                contentColor = LocalCustomColorsPalette.current.contentColorFor(color),
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(buttonText)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ShellStartCard(
    modifier: Modifier = Modifier,
    shellStartCommandState: ShellStartCommandState,
    onGetShellStartCommandClick: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    OutlinedCard(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(
                imageVector = Icons.Rounded.Usb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.expert_mode_shell_start_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.expert_mode_shell_start_description),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (shellStartCommandState) {
            is ShellStartCommandState.Idle -> {
                FilledTonalButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp),
                    onClick = onGetShellStartCommandClick,
                ) {
                    Text(stringResource(R.string.expert_mode_shell_start_get_command))
                }
            }

            is ShellStartCommandState.Loading -> {
                FilledTonalButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp),
                    onClick = {},
                    enabled = false,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.expert_mode_shell_start_get_command))
                }
            }

            is ShellStartCommandState.Loaded -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = shellStartCommandState.command,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val clipEntry = ClipEntry(
                        ClipData.newPlainText(
                            stringResource(
                                R.string.expert_mode_shell_start_clipboard_label,
                            ),
                            shellStartCommandState.command,
                        ),
                    )

                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(clipEntry)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(
                                R.string.expert_mode_shell_start_copy_content_description,
                            ),
                        )
                    }
                }
            }

            is ShellStartCommandState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.expert_mode_shell_start_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onGetShellStartCommandClick,
                    ) {
                        Text(stringResource(R.string.expert_mode_shell_start_retry))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExpertModeInfoCard(modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    OutlinedCard(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.expert_mode_info_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.expert_mode_info_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(
                        R.string.expert_mode_info_card_dismiss_content_description,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        ExpertModeScreen {
            Content(
                warningState = ExpertModeWarningState.Understood,
                setupState = State.Data(
                    ExpertModeState.Stopped(
                        isRootGranted = false,
                        shizukuSetupState = ShizukuSetupState.PERMISSION_GRANTED,
                        isNotificationPermissionGranted = true,
                        isStarting = false,
                        shellStartCommandState = ShellStartCommandState.Idle,
                    ),
                ),
                showInfoCard = true,
                onInfoCardDismiss = {},
                onAutoStartAtBootToggled = {},
                onLaunchDeveloperOptionsClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDark() {
    KeyMapperTheme(darkTheme = true) {
        ExpertModeScreen {
            Content(
                warningState = ExpertModeWarningState.Understood,
                setupState = State.Data(
                    ExpertModeState.Started(
                        isDefaultUsbModeCompatible = true,
                        autoStartBootChecked = true,
                        autoStartBootEnabled = true,
                        isAdbInputSecurityEnabled = null,
                    ),
                ),
                showInfoCard = false,
                onInfoCardDismiss = {},
                onAutoStartAtBootToggled = {},
                onLaunchDeveloperOptionsClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewCountingDown() {
    KeyMapperTheme {
        ExpertModeScreen {
            Content(
                warningState = ExpertModeWarningState.CountingDown(
                    seconds = 5,
                ),
                setupState = State.Loading,
                showInfoCard = true,
                onInfoCardDismiss = {},
                onAutoStartAtBootToggled = {},
                onLaunchDeveloperOptionsClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewStarted() {
    KeyMapperTheme {
        ExpertModeScreen {
            Content(
                warningState = ExpertModeWarningState.Understood,
                setupState = State.Data(
                    ExpertModeState.Started(
                        isDefaultUsbModeCompatible = false,
                        autoStartBootChecked = false,
                        autoStartBootEnabled = true,
                        isAdbInputSecurityEnabled = null,
                    ),
                ),
                showInfoCard = false,
                onInfoCardDismiss = {},
                onAutoStartAtBootToggled = {},
                onLaunchDeveloperOptionsClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNotificationPermissionNotGranted() {
    KeyMapperTheme {
        ExpertModeScreen {
            Content(
                warningState = ExpertModeWarningState.Understood,
                setupState = State.Data(
                    ExpertModeState.Stopped(
                        isRootGranted = true,
                        shizukuSetupState = ShizukuSetupState.PERMISSION_GRANTED,
                        isNotificationPermissionGranted = false,
                        isStarting = false,
                        shellStartCommandState = ShellStartCommandState.Idle,
                    ),
                ),
                showInfoCard = false,
                onInfoCardDismiss = {},
                onAutoStartAtBootToggled = {},
                onLaunchDeveloperOptionsClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewUsbDebuggingSecuritySettingsCard() {
    KeyMapperTheme {
        ExpertModeScreen {
            Content(
                warningState = ExpertModeWarningState.Understood,
                setupState = State.Data(
                    ExpertModeState.Started(
                        isDefaultUsbModeCompatible = true,
                        autoStartBootChecked = false,
                        autoStartBootEnabled = true,
                        isAdbInputSecurityEnabled = false,
                    ),
                ),
                showInfoCard = false,
                onInfoCardDismiss = {},
                onAutoStartAtBootToggled = {},
                onLaunchDeveloperOptionsClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewShellStartCard() {
    KeyMapperTheme {
        ExpertModeScreen {
            Content(
                warningState = ExpertModeWarningState.Understood,
                setupState = State.Data(
                    ExpertModeState.Stopped(
                        isRootGranted = false,
                        shizukuSetupState = ShizukuSetupState.NOT_FOUND,
                        isNotificationPermissionGranted = true,
                        isStarting = false,
                        shellStartCommandState = ShellStartCommandState.Loaded(
                            "sh /storage/emulated/0/Android/data/io.github.sds100.keymapper/files/start.sh",
                        ),
                    ),
                ),
                showInfoCard = false,
                onInfoCardDismiss = {},
                onAutoStartAtBootToggled = {},
                onLaunchDeveloperOptionsClick = {},
            )
        }
    }
}
