@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.sds100.keymapper.base.trigger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandleTriggerSetupBottomSheet(
    viewModel: BaseConfigTriggerViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val triggerSetupState: TriggerSetupState? by viewModel.triggerSetupState.collectAsStateWithLifecycle()

    when (triggerSetupState) {
        is TriggerSetupState.Volume -> VolumeTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Volume,
            onDismissRequest = viewModel::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = viewModel::onEnableAccessibilityServiceClick,
            onEnableProModeClick = viewModel::onEnableProModeClick,
            onRecordTriggerClick = viewModel::onRecordTriggerButtonClick,
            onScreenOffCheckedChange = viewModel::onScreenOffTriggerSetupCheckedChange
        )

        null -> {}
    }
}

@Composable
fun VolumeTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.Volume,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableProModeClick: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
    onScreenOffCheckedChange: (Boolean) -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState, onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_volume_title),
        icon = Icons.AutoMirrored.Outlined.VolumeUp,
        positiveButtonContent = {
            if (state.areRequirementsMet) {
                RecordTriggerButton(
                    modifier = Modifier.weight(1f),
                    state = state.recordTriggerState,
                    onClick = onRecordTriggerClick
                )
            } else {
                RequirementsNotMetButton(modifier = Modifier.weight(1f))
            }
        }
    ) {

        Header(text = stringResource(R.string.trigger_setup_options_title))

        CheckBoxText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.trigger_setup_screen_off_option),
            isChecked = state.isScreenOffChecked,
            isEnabled = true,
            onCheckedChange = onScreenOffCheckedChange
        )

        Header(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick
        )

        AnimatedVisibility(
            visible = state.isScreenOffChecked,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ProModeRequirementRow(
                proModeStatus = state.proModeStatus,
                onClick = onEnableProModeClick
            )
        }
    }
}

@Composable
private fun ProModeRequirementRow(
    modifier: Modifier = Modifier,
    proModeStatus: ProModeStatus,
    onClick: () -> Unit
) {
    RequirementRow(
        modifier = modifier,
        text = stringResource(R.string.trigger_setup_pro_mode_title)
    ) {
        if (proModeStatus == ProModeStatus.UNSUPPORTED) {
            Text(
                text = stringResource(R.string.trigger_setup_pro_mode_unsupported),
                color = MaterialTheme.colorScheme.error
            )
        } else {
            RequirementButton(
                enabledText = stringResource(R.string.trigger_setup_pro_mode_enable_button),
                disabledText = stringResource(R.string.trigger_setup_pro_mode_running_button),
                isEnabled = proModeStatus != ProModeStatus.ENABLED,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun AccessibilityServiceRequirementRow(
    modifier: Modifier = Modifier,
    isServiceEnabled: Boolean,
    onClick: () -> Unit
) {
    RequirementRow(
        modifier = modifier,
        text = stringResource(R.string.trigger_setup_accessibility_service_title)
    ) {
        RequirementButton(
            enabledText = stringResource(R.string.trigger_setup_accessibility_service_enable_button),
            disabledText = stringResource(R.string.trigger_setup_accessibility_service_running_button),
            isEnabled = !isServiceEnabled,
            onClick = onClick
        )
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun RequirementRow(
    modifier: Modifier = Modifier,
    text: String,
    actionContent: @Composable () -> Unit
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )

        actionContent()
    }
}

@Composable
private fun RequirementsNotMetButton(modifier: Modifier = Modifier) {
    FilledTonalButton(modifier = modifier, onClick = {}, enabled = false) {
        Text(stringResource(R.string.trigger_setup_requirements_not_met_button))
    }
}

@Composable
private fun RequirementButton(
    modifier: Modifier = Modifier,
    enabledText: String,
    disabledText: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        enabled = isEnabled
    ) {
        if (isEnabled) {
            Text(text = enabledText)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = disabledText)
            }
        }
    }
}

@Composable
private fun TriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector,
    positiveButtonContent: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title, style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            content()

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.neg_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                positiveButtonContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun VolumeButtonPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        VolumeTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Volume(
                isAccessibilityServiceEnabled = true,
                isScreenOffChecked = true,
                proModeStatus = ProModeStatus.ENABLED,
                areRequirementsMet = true,
                recordTriggerState = RecordTriggerState.Idle,
                remapStatus = RemapStatus.SUPPORTED
            )
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun VolumeButtonDisabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        VolumeTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Volume(
                isAccessibilityServiceEnabled = false,
                isScreenOffChecked = true,
                proModeStatus = ProModeStatus.DISABLED,
                areRequirementsMet = false,
                recordTriggerState = RecordTriggerState.Idle,
                remapStatus = RemapStatus.UNCERTAIN
            )
        )
    }

}