package io.github.sds100.keymapper.base.promode

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep

@Composable
fun ProModeSetupScreen(
    viewModel: ProModeSetupViewModel,
) {
    val state by viewModel.setupState.collectAsStateWithLifecycle()

    ProModeSetupScreen(
        state = state,
        onStepButtonClick = viewModel::onStepButtonClick,
        onAssistantClick = viewModel::onAssistantClick,
        onWatchTutorialClick = { }, //TODO
        onBackClick = viewModel::onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProModeSetupScreen(
    state: State<ProModeSetupState>,
    onBackClick: () -> Unit = {},
    onStepButtonClick: () -> Unit = {},
    onAssistantClick: () -> Unit = {},
    onWatchTutorialClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pro_mode_setup_wizard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_go_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (state) {
            State.Loading -> {
                CircularProgressIndicator()
            }

            is State.Data -> {
                val stepContent = getStepContent(state.data.step)

                // Create animated progress for entrance and updates
                val progressAnimatable = remember { Animatable(0f) }
                val targetProgress = state.data.stepNumber.toFloat() / (state.data.stepCount)

                // Animate progress when it changes
                LaunchedEffect(targetProgress) {
                    progressAnimatable.animateTo(
                        targetValue = targetProgress,
                        animationSpec = tween(
                            durationMillis = 800,
                            easing = EaseInOut
                        )
                    )
                }

                // Animate entrance when screen opens
                LaunchedEffect(Unit) {
                    progressAnimatable.animateTo(
                        targetValue = targetProgress,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = EaseInOut
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { progressAnimatable.value }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.pro_mode_setup_wizard_step_n,
                                state.data.stepNumber,
                                state.data.stepCount
                            ),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.pro_mode_app_bar_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    AssistantCheckBoxRow(
                        modifier = Modifier.fillMaxWidth(),
                        isEnabled = state.data.isSetupAssistantButtonEnabled,
                        isChecked = state.data.isSetupAssistantChecked,
                        onAssistantClick = onAssistantClick
                    )

                    val iconTint = if (state.data.step == SystemBridgeSetupStep.STARTED) {
                        LocalCustomColorsPalette.current.green
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    StepContent(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        stepContent,
                        onWatchTutorialClick,
                        onStepButtonClick,
                        iconTint = iconTint
                    )
                }
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
) {
    Column(
        modifier,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = stepContent.icon,
                contentDescription = null,
                tint = iconTint
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepContent.title,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepContent.message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onWatchTutorialClick) {
                Text(text = stringResource(R.string.pro_mode_setup_wizard_watch_tutorial_button))
            }
            Button(onClick = onButtonClick) {
                Text(text = stepContent.buttonText)
            }
        }
    }
}

@Composable
private fun AssistantCheckBoxRow(
    modifier: Modifier,
    isEnabled: Boolean,
    isChecked: Boolean,
    onAssistantClick: () -> Unit
) {
    Surface(
        modifier = modifier, shape = MaterialTheme.shapes.medium,
        enabled = isEnabled,
        onClick = onAssistantClick
    ) {
        val contentColor = if (isEnabled) {
            LocalContentColor.current
        } else {
            LocalContentColor.current.copy(alpha = 0.5f)
        }

        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    enabled = isEnabled,
                    checked = isChecked,
                    onCheckedChange = { onAssistantClick() })
                Column {
                    Text(
                        text = stringResource(R.string.pro_mode_setup_wizard_use_assistant),
                        style = MaterialTheme.typography.titleMedium
                    )

                    val text = if (isEnabled) {
                        stringResource(R.string.pro_mode_setup_wizard_use_assistant_description)
                    } else {
                        stringResource(R.string.pro_mode_setup_wizard_use_assistant_enable_accessibility_service)
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun getStepContent(step: SystemBridgeSetupStep): StepContent {
    return when (step) {
        SystemBridgeSetupStep.ACCESSIBILITY_SERVICE -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_enable_accessibility_service_title),
            message = stringResource(R.string.pro_mode_setup_wizard_enable_accessibility_service_description),
            icon = Icons.Rounded.Accessibility,
            buttonText = stringResource(R.string.pro_mode_setup_wizard_enable_accessibility_service_button)
        )

        SystemBridgeSetupStep.NOTIFICATION_PERMISSION -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_enable_notification_permission_title),
            message = stringResource(R.string.pro_mode_setup_wizard_enable_notification_permission_description),
            icon = Icons.Rounded.Notifications,
            buttonText = stringResource(R.string.pro_mode_setup_wizard_enable_notification_permission_button)
        )

        SystemBridgeSetupStep.DEVELOPER_OPTIONS -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_enable_developer_options_title),
            message = stringResource(R.string.pro_mode_setup_wizard_enable_developer_options_description),
            icon = Icons.Rounded.Build,
            buttonText = stringResource(R.string.pro_mode_setup_wizard_go_to_settings_button)
        )

        SystemBridgeSetupStep.WIFI_NETWORK -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_connect_wifi_title),
            message = stringResource(R.string.pro_mode_setup_wizard_connect_wifi_description),
            icon = KeyMapperIcons.SignalWifiNotConnected,
            buttonText = stringResource(R.string.pro_mode_setup_wizard_go_to_settings_button)
        )

        SystemBridgeSetupStep.WIRELESS_DEBUGGING -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_enable_wireless_debugging_title),
            message = stringResource(R.string.pro_mode_setup_wizard_enable_wireless_debugging_description),
            icon = Icons.Rounded.BugReport,
            buttonText = stringResource(R.string.pro_mode_setup_wizard_go_to_settings_button)
        )

        SystemBridgeSetupStep.ADB_PAIRING -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_pair_wireless_debugging_title),
            message = stringResource(R.string.pro_mode_setup_wizard_pair_wireless_debugging_description),
            icon = Icons.Rounded.Link,
            buttonText = stringResource(R.string.pro_mode_setup_wizard_go_to_settings_button)
        )

        SystemBridgeSetupStep.START_SERVICE -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_start_service_title),
            message = stringResource(R.string.pro_mode_setup_wizard_start_service_description),
            icon = Icons.Rounded.PlayArrow,
            buttonText = stringResource(R.string.pro_mode_root_detected_button_start_service)
        )

        SystemBridgeSetupStep.STARTED -> StepContent(
            title = stringResource(R.string.pro_mode_setup_wizard_complete_title),
            message = stringResource(R.string.pro_mode_setup_wizard_complete_text),
            icon = Icons.Rounded.CheckCircleOutline,
            buttonText = stringResource(R.string.pro_mode_setup_wizard_complete_button)
        )
    }
}

private data class StepContent(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val buttonText: String,
)

// Previews for each setup step
@Preview(name = "Accessibility Service Step")
@Composable
private fun ProModeSetupScreenAccessibilityServicePreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 1,
                    stepCount = 6,
                    step = SystemBridgeSetupStep.ACCESSIBILITY_SERVICE,
                    isSetupAssistantChecked = false,
                    isSetupAssistantButtonEnabled = false
                )
            )
        )
    }
}

@Preview(name = "Notification Permission Step")
@Composable
private fun ProModeSetupScreenNotificationPermissionPreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 2,
                    stepCount = 6,
                    step = SystemBridgeSetupStep.NOTIFICATION_PERMISSION,
                    isSetupAssistantChecked = false,
                    isSetupAssistantButtonEnabled = true
                )
            )
        )
    }
}

@Preview(name = "Developer Options Step")
@Composable
private fun ProModeSetupScreenDeveloperOptionsPreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 2,
                    stepCount = 6,
                    step = SystemBridgeSetupStep.DEVELOPER_OPTIONS,
                    isSetupAssistantChecked = false,
                    isSetupAssistantButtonEnabled = true
                )
            )
        )
    }
}

@Preview(name = "WiFi Network Step")
@Composable
private fun ProModeSetupScreenWifiNetworkPreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 3,
                    stepCount = 6,
                    step = SystemBridgeSetupStep.WIFI_NETWORK,
                    isSetupAssistantChecked = false,
                    isSetupAssistantButtonEnabled = true
                )
            )
        )
    }
}

@Preview(name = "Wireless Debugging Step")
@Composable
private fun ProModeSetupScreenWirelessDebuggingPreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 4,
                    stepCount = 6,
                    step = SystemBridgeSetupStep.WIRELESS_DEBUGGING,
                    isSetupAssistantChecked = false,
                    isSetupAssistantButtonEnabled = true
                )
            )
        )
    }
}

@Preview(name = "ADB Pairing Step", widthDp = 400, heightDp = 400)
@Composable
private fun ProModeSetupScreenAdbPairingPreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 5,
                    stepCount = 6,
                    step = SystemBridgeSetupStep.ADB_PAIRING,
                    isSetupAssistantChecked = true,
                    isSetupAssistantButtonEnabled = true
                )
            )
        )
    }
}

@Preview(name = "Start Service Step", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProModeSetupScreenStartServicePreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 6,
                    stepCount = 6,
                    step = SystemBridgeSetupStep.START_SERVICE,
                    isSetupAssistantChecked = true,
                    isSetupAssistantButtonEnabled = true
                )
            )
        )
    }
}


@Preview(name = "Started", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProModeSetupScreenStartedPreview() {
    KeyMapperTheme {
        ProModeSetupScreen(
            state = State.Data(
                ProModeSetupState(
                    stepNumber = 8,
                    stepCount = 8,
                    step = SystemBridgeSetupStep.STARTED,
                    isSetupAssistantChecked = true,
                    isSetupAssistantButtonEnabled = true
                )
            )
        )
    }
}

