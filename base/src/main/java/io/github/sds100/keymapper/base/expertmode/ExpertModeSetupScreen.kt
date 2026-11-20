package io.github.sds100.keymapper.base.expertmode

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.SignalWifiNotConnected
import io.github.sds100.keymapper.common.utils.State

@Composable
fun ExpertModeSetupScreen(viewModel: ExpertModeSetupViewModel) {
    val state by viewModel.setupState.collectAsStateWithLifecycle()

    ExpertModeSetupScreen(
        state = state,
        onStepButtonClick = viewModel::onSetupStepButtonClick,
        onAssistantClick = viewModel::onSetupAssistantClick,
        onWatchTutorialClick = { },
        onBackClick = viewModel::onBackClick,
        onSamsungAutoBlockerWarningClick = viewModel::onSamsungAutoBlockerWarningClick,
        onSkipDeveloperOptionsClick = viewModel::onSkipDeveloperOptionsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertModeSetupScreen(
    state: State<ExpertModeSetupState>,
    onBackClick: () -> Unit = {},
    onStepButtonClick: () -> Unit = {},
    onAssistantClick: () -> Unit = {},
    onWatchTutorialClick: () -> Unit = {},
    onSamsungAutoBlockerWarningClick: () -> Unit = {},
    onSkipDeveloperOptionsClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expert_mode_setup_wizard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_go_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        ExpertModeSetupScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state,
            onAssistantClick,
            onWatchTutorialClick,
            onStepButtonClick,
            onSamsungAutoBlockerWarningClick,
            onSkipDeveloperOptionsClick,
        )
    }
}

@Composable
fun ExpertModeSetupScreenContent(
    modifier: Modifier = Modifier,
    state: State<ExpertModeSetupState>,
    onAssistantClick: () -> Unit,
    onWatchTutorialClick: () -> Unit,
    onStepButtonClick: () -> Unit,
    onSamsungAutoBlockerWarningClick: () -> Unit = {},
    onSkipDeveloperOptionsClick: () -> Unit = {},
) {
    when (state) {
        State.Loading -> {
            Box(
                modifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is State.Data -> {
            val stepData = state.data
            val stepContent = getStepContent(stepData)

            // Create animated progress for entrance and updates
            val progressAnimatable = remember { Animatable(0f) }
            val targetProgress = stepData.stepNumber.toFloat() / (stepData.stepCount)

            // Animate progress when it changes
            LaunchedEffect(targetProgress) {
                progressAnimatable.animateTo(
                    targetValue = targetProgress,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = EaseInOut,
                    ),
                )
            }

            // Animate entrance when screen opens
            LaunchedEffect(Unit) {
                progressAnimatable.animateTo(
                    targetValue = targetProgress,
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = EaseInOut,
                    ),
                )
            }

            Column(
                modifier = modifier
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { progressAnimatable.value },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.expert_mode_setup_wizard_step_n,
                            stepData.stepNumber,
                            stepData.stepCount,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.expert_mode_setup_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                AssistantCheckBoxRow(
                    modifier = Modifier.fillMaxWidth(),
                    isEnabled = stepData.isSetupAssistantButtonEnabled,
                    isChecked = stepData.isSetupAssistantChecked,
                    onAssistantClick = onAssistantClick,
                )

                val iconTint = if (stepData is ExpertModeSetupState.Started) {
                    LocalCustomColorsPalette.current.green
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                val isStarting = stepData is ExpertModeSetupState.StartService &&
                    stepData.isStarting

                StepContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    stepContent = stepContent,
                    onWatchTutorialClick = onWatchTutorialClick,
                    onButtonClick = onStepButtonClick,
                    iconTint = iconTint,
                    isLoading = isStarting,
                    warningContent = when {
                        stepData is ExpertModeSetupState.WirelessDebugging &&
                            stepData.showSamsungAutoBlockerWarning -> {
                            {
                                SamsungAutoBlockerWarningCard(
                                    onClick = onSamsungAutoBlockerWarningClick,
                                )
                            }
                        }

                        stepData is ExpertModeSetupState.DeveloperOptions &&
                            stepData.showRedactedWarning -> {
                            {
                                DeveloperOptionsRedactedWarningCard(
                                    onClick = onSkipDeveloperOptionsClick,
                                )
                            }
                        }

                        else -> null
                    },
                )
            }
        }
    }
}

@Composable
private fun StepContent(
    modifier: Modifier = Modifier,
    stepContent: StepContent,
    onWatchTutorialClick: () -> Unit,
    onButtonClick: () -> Unit,
    iconTint: Color = Color.Unspecified,
    isLoading: Boolean = false,
    warningContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                )
            } else {
                Icon(
                    modifier = Modifier.size(64.dp),
                    imageVector = stepContent.icon,
                    contentDescription = null,
                    tint = iconTint,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepContent.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepContent.message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (warningContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                warningContent()
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
//            TextButton(onClick = onWatchTutorialClick) {
//                Text(text = stringResource(R.string.expert_mode_setup_wizard_watch_tutorial_button))
//            }
            Button(
                onClick = onButtonClick,
                enabled = !isLoading,
            ) {
                Text(text = stepContent.buttonText)
            }
        }
    }
}

@Composable
private fun SamsungAutoBlockerWarningCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    SetupWarningCard(
        modifier = modifier,
        title = stringResource(
            R.string.expert_mode_setup_wizard_samsung_auto_blocker_warning_title,
        ),
        description = stringResource(
            R.string.expert_mode_setup_wizard_samsung_auto_blocker_warning_description,
        ),
        buttonText = stringResource(R.string.expert_mode_setup_wizard_go_to_settings_button),
        onButtonClick = onClick,
    )
}

@Composable
private fun DeveloperOptionsRedactedWarningCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SetupWarningCard(
        modifier = modifier,
        title = stringResource(
            R.string.expert_mode_setup_wizard_developer_options_redacted_warning_title,
        ),
        description = stringResource(
            R.string.expert_mode_setup_wizard_developer_options_redacted_warning_description,
        ),
        buttonText = stringResource(
            R.string.expert_mode_setup_wizard_developer_options_redacted_warning_button,
        ),
        onButtonClick = onClick,
    )
}

@Composable
private fun SetupWarningCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(onClick = onButtonClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun AssistantCheckBoxRow(
    modifier: Modifier,
    isEnabled: Boolean,
    isChecked: Boolean,
    onAssistantClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        enabled = isEnabled,
        onClick = onAssistantClick,
    ) {
        val contentColor = if (isEnabled) {
            LocalContentColor.current
        } else {
            LocalContentColor.current.copy(alpha = 0.5f)
        }

        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    enabled = isEnabled,
                    checked = isChecked,
                    onCheckedChange = { onAssistantClick() },
                )

                val text = if (isEnabled) {
                    stringResource(R.string.expert_mode_setup_wizard_use_assistant_description)
                } else {
                    stringResource(
                        R.string.expert_mode_setup_wizard_use_assistant_enable_service,
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.expert_mode_setup_wizard_use_assistant),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun getStepContent(state: ExpertModeSetupState): StepContent {
    return when (state) {
        is ExpertModeSetupState.AccessibilityService -> StepContent(
            title = stringResource(
                R.string.expert_mode_setup_wizard_enable_accessibility_service_title,
            ),
            message = stringResource(
                R.string.expert_mode_setup_wizard_enable_accessibility_service_description,
            ),
            icon = Icons.Rounded.Accessibility,
            buttonText = stringResource(
                R.string.expert_mode_setup_wizard_enable_accessibility_service_button,
            ),
        )

        is ExpertModeSetupState.NotificationPermission -> StepContent(
            title = stringResource(
                R.string.expert_mode_setup_wizard_enable_notification_permission_title,
            ),
            message = stringResource(
                R.string.expert_mode_setup_wizard_enable_notification_permission_description,
            ),
            icon = Icons.Rounded.Notifications,
            buttonText = stringResource(
                R.string.expert_mode_setup_wizard_enable_notification_permission_button,
            ),
        )

        is ExpertModeSetupState.LocalNetworkPermission -> StepContent(
            title = stringResource(
                R.string.expert_mode_setup_wizard_local_network_permission_title,
            ),
            message = stringResource(
                R.string.expert_mode_setup_wizard_local_network_permission_description,
            ),
            icon = Icons.Rounded.Lan,
            buttonText = stringResource(
                R.string.expert_mode_setup_wizard_local_network_permission_button,
            ),
        )

        is ExpertModeSetupState.SamsungAutoBlocker -> StepContent(
            title = stringResource(
                R.string.expert_mode_setup_wizard_disable_samsung_auto_blocker_title,
            ),
            message = stringResource(
                R.string.expert_mode_setup_wizard_disable_samsung_auto_blocker_description,
            ),
            icon = Icons.Rounded.Security,
            buttonText = stringResource(R.string.expert_mode_setup_wizard_go_to_settings_button),
        )

        is ExpertModeSetupState.DeveloperOptions -> StepContent(
            title = stringResource(
                R.string.expert_mode_setup_wizard_enable_developer_options_title,
            ),
            message = stringResource(
                R.string.expert_mode_setup_wizard_enable_developer_options_description,
            ),
            icon = Icons.Rounded.Build,
            buttonText = stringResource(R.string.expert_mode_setup_wizard_go_to_settings_button),
        )

        is ExpertModeSetupState.WifiNetwork -> StepContent(
            title = stringResource(R.string.expert_mode_setup_wizard_connect_wifi_title),
            message = stringResource(R.string.expert_mode_setup_wizard_connect_wifi_description),
            icon = KeyMapperIcons.SignalWifiNotConnected,
            buttonText = stringResource(R.string.expert_mode_setup_wizard_go_to_settings_button),
        )

        is ExpertModeSetupState.WirelessDebugging -> StepContent(
            title = stringResource(
                R.string.expert_mode_setup_wizard_enable_wireless_debugging_title,
            ),
            message = stringResource(
                R.string.expert_mode_setup_wizard_enable_wireless_debugging_description,
            ),
            icon = Icons.Rounded.BugReport,
            buttonText = stringResource(R.string.expert_mode_setup_wizard_go_to_settings_button),
        )

        is ExpertModeSetupState.AdbPairing -> StepContent(
            title = stringResource(R.string.expert_mode_setup_wizard_pair_wireless_debugging_title),
            message = stringResource(
                R.string.expert_mode_setup_wizard_pair_wireless_debugging_description,
            ),
            icon = Icons.Rounded.Link,
            buttonText = stringResource(R.string.expert_mode_setup_wizard_go_to_settings_button),
        )

        is ExpertModeSetupState.StartService -> StepContent(
            title = stringResource(R.string.expert_mode_setup_wizard_start_service_title),
            message = stringResource(R.string.expert_mode_setup_wizard_start_service_description),
            icon = Icons.Rounded.PlayArrow,
            buttonText = stringResource(R.string.expert_mode_root_detected_button_start_service),
        )

        is ExpertModeSetupState.Started -> StepContent(
            title = stringResource(R.string.expert_mode_setup_wizard_complete_title),
            message = stringResource(R.string.expert_mode_setup_wizard_complete_text),
            icon = Icons.Rounded.CheckCircleOutline,
            buttonText = stringResource(R.string.expert_mode_setup_wizard_complete_button),
        )
    }
}

@Preview(name = "Accessibility Service Step")
@Composable
private fun ExpertModeSetupScreenAccessibilityServicePreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.AccessibilityService(
                    stepNumber = 1,
                    stepCount = 10,
                ),
            ),
        )
    }
}

@Preview(name = "Notification Permission Step")
@Composable
private fun ExpertModeSetupScreenNotificationPermissionPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.NotificationPermission(
                    stepNumber = 2,
                    stepCount = 10,
                    isSetupAssistantChecked = false,
                ),
            ),
        )
    }
}

@Preview(name = "Local Network Permission Step")
@Composable
private fun ExpertModeSetupScreenLocalNetworkPermissionPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.LocalNetworkPermission(
                    stepNumber = 3,
                    stepCount = 10,
                    isSetupAssistantChecked = false,
                ),
            ),
        )
    }
}

@Preview(name = "Samsung Auto Blocker Step")
@Composable
private fun ExpertModeSetupScreenSamsungAutoBlockerPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.SamsungAutoBlocker(
                    stepNumber = 4,
                    stepCount = 10,
                    isSetupAssistantChecked = false,
                ),
            ),
        )
    }
}

@Preview(name = "Developer Options Step")
@Composable
private fun ExpertModeSetupScreenDeveloperOptionsPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.DeveloperOptions(
                    stepNumber = 5,
                    stepCount = 10,
                    isSetupAssistantChecked = false,
                    showRedactedWarning = true,
                ),
            ),
        )
    }
}

@Preview(name = "WiFi Network Step")
@Composable
private fun ExpertModeSetupScreenWifiNetworkPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.WifiNetwork(
                    stepNumber = 6,
                    stepCount = 10,
                    isSetupAssistantChecked = false,
                ),
            ),
        )
    }
}

@Preview(name = "Wireless Debugging Step")
@Composable
private fun ExpertModeSetupScreenWirelessDebuggingPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.WirelessDebugging(
                    stepNumber = 7,
                    stepCount = 10,
                    isSetupAssistantChecked = false,
                    showSamsungAutoBlockerWarning = false,
                ),
            ),
        )
    }
}

@Preview(name = "Wireless Debugging Step (Auto Blocker Warning)")
@Composable
private fun ExpertModeSetupScreenWirelessDebuggingAutoBlockerWarningPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.WirelessDebugging(
                    stepNumber = 7,
                    stepCount = 10,
                    isSetupAssistantChecked = false,
                    showSamsungAutoBlockerWarning = true,
                ),
            ),
        )
    }
}

@Preview(name = "ADB Pairing Step", widthDp = 400, heightDp = 400)
@Composable
private fun ExpertModeSetupScreenAdbPairingPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.AdbPairing(
                    stepNumber = 8,
                    stepCount = 10,
                    isSetupAssistantChecked = true,
                ),
            ),
        )
    }
}

@Preview(name = "Start Service Step", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpertModeSetupScreenStartServicePreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.StartService(
                    stepNumber = 9,
                    stepCount = 10,
                    isSetupAssistantChecked = true,
                    isStarting = false,
                ),
            ),
        )
    }
}

@Preview(name = "Started", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpertModeSetupScreenStartedPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Data(
                ExpertModeSetupState.Started(
                    stepNumber = 10,
                    stepCount = 10,
                    isSetupAssistantChecked = true,
                ),
            ),
        )
    }
}

@Preview(name = "Loading", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpertModeSetupScreenLoadingPreview() {
    KeyMapperTheme {
        ExpertModeSetupScreen(
            state = State.Loading,
        )
    }
}
