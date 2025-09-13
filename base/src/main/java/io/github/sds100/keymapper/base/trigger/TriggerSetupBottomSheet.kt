@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.sds100.keymapper.base.trigger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.HeaderText
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.ModeOffOn
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
            onRecordTriggerClick = viewModel::onTriggerSetupRecordClick,
            onScreenOffCheckedChange = viewModel::onScreenOffTriggerSetupCheckedChange,
        )

        is TriggerSetupState.Power -> PowerTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Power,
            onDismissRequest = viewModel::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = viewModel::onEnableAccessibilityServiceClick,
            onEnableProModeClick = viewModel::onEnableProModeClick,
            onRecordTriggerClick = viewModel::onTriggerSetupRecordClick,
        )

        is TriggerSetupState.FingerprintGesture -> FingerprintGestureSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.FingerprintGesture,
            onDismissRequest = viewModel::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = viewModel::onEnableAccessibilityServiceClick,
            onGestureTypeSelected = viewModel::onFingerprintGestureTypeSelected,
            onAddTriggerClick = viewModel::onAddFingerprintGestureClick,
        )

        null -> {}
    }
}

@Composable
private fun PowerTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.Power,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableProModeClick: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_power_title),
        icon = KeyMapperIcons.ModeOffOn,
        positiveButtonContent = {
            if (state.areRequirementsMet) {
                RecordTriggerButton(
                    modifier = Modifier.weight(1f),
                    state = state.recordTriggerState,
                    onClick = onRecordTriggerClick,
                )
            } else {
                TriggerRequirementsNotMetButton(modifier = Modifier.weight(1f))
            }
        },
    ) {

        RemapStatusButton(modifier = Modifier.fillMaxWidth(), remapStatus = state.remapStatus)

        HeaderText(text = stringResource(R.string.trigger_setup_options_title))

        CheckBoxText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.trigger_setup_screen_off_option),
            isChecked = true,
            isEnabled = false,
            onCheckedChange = {},
        )

        HeaderText(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick,
        )

        ProModeRequirementRow(
            isVisible = true,
            proModeStatus = state.proModeStatus,
            onClick = onEnableProModeClick,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_information_title))

        Text(
            stringResource(R.string.trigger_setup_power_information),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RemapStatusButton(modifier: Modifier = Modifier, remapStatus: RemapStatus) {
    when (remapStatus) {
        RemapStatus.UNSUPPORTED -> RemapStatusRow(
            modifier = modifier,
            color = MaterialTheme.colorScheme.error,
            text = stringResource(R.string.trigger_setup_status_can_not_remap)
        )

        RemapStatus.UNCERTAIN -> RemapStatusRow(
            modifier = modifier,
            color = LocalCustomColorsPalette.current.amber,
            text = stringResource(R.string.trigger_setup_status_remap_button_possible)
        )

        RemapStatus.SUPPORTED -> RemapStatusRow(
            modifier = modifier,
            color = LocalCustomColorsPalette.current.green,
            text = stringResource(R.string.trigger_setup_status_remap_button_possible)
        )
    }
}

@Composable
private fun VolumeTriggerSetupBottomSheet(
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
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_volume_title),
        icon = Icons.AutoMirrored.Outlined.VolumeUp,

        positiveButtonContent = {
            if (state.areRequirementsMet) {
                RecordTriggerButton(
                    modifier = Modifier.weight(1f),
                    state = state.recordTriggerState,
                    onClick = onRecordTriggerClick,
                )
            } else {
                TriggerRequirementsNotMetButton(modifier = Modifier.weight(1f))
            }
        },
    ) {
        RemapStatusRow(
            modifier = Modifier.fillMaxWidth(),
            color = LocalCustomColorsPalette.current.green,
            text = stringResource(R.string.trigger_setup_status_remap_button_possible)
        )

        HeaderText(text = stringResource(R.string.trigger_setup_options_title))

        CheckBoxText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.trigger_setup_screen_off_option),
            isChecked = state.isScreenOffChecked,
            isEnabled = true,
            onCheckedChange = onScreenOffCheckedChange,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick,
        )

        ProModeRequirementRow(
            isVisible = state.isScreenOffChecked,
            proModeStatus = state.proModeStatus,
            onClick = onEnableProModeClick,
        )
    }
}

@Composable
private fun FingerprintGestureSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.FingerprintGesture,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onGestureTypeSelected: (FingerprintGestureType) -> Unit = {},
    onAddTriggerClick: () -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_fingerprint_reader_title),
        icon = Icons.Rounded.Fingerprint,

        positiveButtonContent = {
            if (state.areRequirementsMet) {
                AddTriggerButton(modifier = Modifier.weight(1f), onClick = onAddTriggerClick)
            } else {
                TriggerRequirementsNotMetButton(modifier = Modifier.weight(1f))
            }
        },
    ) {
        RemapStatusRow(
            modifier = Modifier.fillMaxWidth(),
            color = LocalCustomColorsPalette.current.amber,
            text = stringResource(R.string.trigger_setup_status_might_remap_button)
        )

        HeaderText(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick,
        )

        HeaderText(text = stringResource(R.string.trigger_key_fingerprint_gesture_type_header))

        RadioButtonText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.fingerprint_gesture_down),
            isSelected = state.selectedType == FingerprintGestureType.SWIPE_DOWN,
            onSelected = { onGestureTypeSelected(FingerprintGestureType.SWIPE_DOWN) },
        )

        RadioButtonText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.fingerprint_gesture_up),
            isSelected = state.selectedType == FingerprintGestureType.SWIPE_UP,
            onSelected = { onGestureTypeSelected(FingerprintGestureType.SWIPE_UP) },
        )

        RadioButtonText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.fingerprint_gesture_left),
            isSelected = state.selectedType == FingerprintGestureType.SWIPE_LEFT,
            onSelected = { onGestureTypeSelected(FingerprintGestureType.SWIPE_LEFT) },
        )

        RadioButtonText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.fingerprint_gesture_right),
            isSelected = state.selectedType == FingerprintGestureType.SWIPE_RIGHT,
            onSelected = { onGestureTypeSelected(FingerprintGestureType.SWIPE_RIGHT) },
        )
    }
}

@Composable
fun RemapStatusRow(
    modifier: Modifier = Modifier,
    color: Color,
    text: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = color,
                    shape = CircleShape,
                ),
        )

        Spacer(Modifier.width(16.dp))

        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ProModeRequirementRow(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    proModeStatus: ProModeStatus,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200)),
    ) {
        TriggerRequirementRow(
            modifier = modifier,
            text = stringResource(R.string.trigger_setup_pro_mode_title),
        ) {
            if (proModeStatus == ProModeStatus.UNSUPPORTED) {
                Text(
                    text = stringResource(R.string.trigger_setup_pro_mode_unsupported),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                TriggerRequirementButton(
                    enabledText = stringResource(R.string.trigger_setup_pro_mode_enable_button),
                    disabledText = stringResource(R.string.trigger_setup_pro_mode_running_button),
                    isEnabled = proModeStatus != ProModeStatus.ENABLED,
                    onClick = onClick,
                )
            }
        }
    }
}

@Composable
fun AccessibilityServiceRequirementRow(
    modifier: Modifier = Modifier,
    isServiceEnabled: Boolean,
    onClick: () -> Unit,
) {
    TriggerRequirementRow(
        modifier = modifier,
        text = stringResource(R.string.trigger_setup_accessibility_service_title),
    ) {
        TriggerRequirementButton(
            enabledText = stringResource(R.string.trigger_setup_accessibility_service_enable_button),
            disabledText = stringResource(R.string.trigger_setup_accessibility_service_running_button),
            isEnabled = !isServiceEnabled,
            onClick = onClick,
        )
    }
}

@Composable
fun TriggerRequirementRow(
    modifier: Modifier = Modifier,
    text: String,
    actionContent: @Composable () -> Unit,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )

        actionContent()
    }
}

@Composable
fun TriggerRequirementsNotMetButton(modifier: Modifier = Modifier) {
    FilledTonalButton(modifier = modifier, onClick = {}, enabled = false) {
        Text(stringResource(R.string.trigger_setup_requirements_not_met_button))
    }
}


@Composable
fun AddTriggerButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(modifier = modifier, onClick = onClick) {
        Text(stringResource(R.string.trigger_setup_add_trigger_button))
    }
}

@Composable
fun TriggerRequirementButton(
    modifier: Modifier = Modifier,
    enabledText: String,
    disabledText: String,
    isEnabled: Boolean,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        enabled = isEnabled,
        colors = colors
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
fun TriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector,
    positiveButtonContent: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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
                horizontalArrangement = Arrangement.Center,
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                modifier = Modifier
                    .animateContentSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }

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
private fun PowerButtonPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        PowerTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Power(
                isAccessibilityServiceEnabled = true,
                proModeStatus = ProModeStatus.ENABLED,
                areRequirementsMet = true,
                recordTriggerState = RecordTriggerState.Idle,
                remapStatus = RemapStatus.SUPPORTED,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PowerButtonDisabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        PowerTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Power(
                isAccessibilityServiceEnabled = false,
                proModeStatus = ProModeStatus.UNSUPPORTED,
                areRequirementsMet = false,
                recordTriggerState = RecordTriggerState.Idle,
                remapStatus = RemapStatus.UNSUPPORTED,
            ),
        )
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
            ),
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
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun FingerprintGestureRequirementsMetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        FingerprintGestureSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.FingerprintGesture(
                isAccessibilityServiceEnabled = true,
                areRequirementsMet = true,
                selectedType = FingerprintGestureType.SWIPE_DOWN,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun FingerprintGestureRequirementsNotMetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        FingerprintGestureSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.FingerprintGesture(
                isAccessibilityServiceEnabled = false,
                areRequirementsMet = false,
                selectedType = FingerprintGestureType.SWIPE_UP,
            ),
        )
    }
}
