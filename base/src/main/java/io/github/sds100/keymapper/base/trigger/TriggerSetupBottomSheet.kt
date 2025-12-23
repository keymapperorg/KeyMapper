@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.sds100.keymapper.base.trigger

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.utils.ExpertModeStatus
import io.github.sds100.keymapper.base.utils.ui.compose.AccessibilityServiceRequirementRow
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.ExpertModeRequirementRow
import io.github.sds100.keymapper.base.utils.ui.compose.HeaderText
import io.github.sds100.keymapper.base.utils.ui.compose.InputMethodRequirementRow
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import io.github.sds100.keymapper.base.utils.ui.compose.icons.IndeterminateQuestionBox
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.ModeOffOn
import io.github.sds100.keymapper.base.utils.ui.compose.icons.SportsEsports
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandleTriggerSetupBottomSheet(delegate: TriggerSetupDelegate) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val triggerSetupState: TriggerSetupState? by
        delegate.triggerSetupState.collectAsStateWithLifecycle()

    when (triggerSetupState) {
        is TriggerSetupState.Volume -> VolumeTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Volume,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onEnableExpertModeClick = delegate::onEnableExpertModeClick,
            onRecordTriggerClick = delegate::onTriggerSetupRecordClick,
            onUseExpertModeCheckedChange = delegate::onUseExpertModeCheckedChange,
        )

        is TriggerSetupState.Power -> PowerTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Power,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onEnableExpertModeClick = delegate::onEnableExpertModeClick,
            onRecordTriggerClick = delegate::onTriggerSetupRecordClick,
        )

        is TriggerSetupState.FingerprintGesture -> FingerprintGestureSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.FingerprintGesture,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onGestureTypeSelected = delegate::onFingerprintGestureTypeSelected,
            onAddTriggerClick = delegate::onAddFingerprintGestureClick,
        )

        is TriggerSetupState.Keyboard -> KeyboardTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Keyboard,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onEnableExpertModeClick = delegate::onEnableExpertModeClick,
            onRecordTriggerClick = delegate::onTriggerSetupRecordClick,
            onUseExpertModeCheckedChange = delegate::onUseExpertModeCheckedChange,
        )

        is TriggerSetupState.Mouse -> MouseTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Mouse,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onEnableExpertModeClick = delegate::onEnableExpertModeClick,
            onRecordTriggerClick = delegate::onTriggerSetupRecordClick,
        )

        is TriggerSetupState.Other -> OtherTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Other,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onEnableExpertModeClick = delegate::onEnableExpertModeClick,
            onRecordTriggerClick = delegate::onTriggerSetupRecordClick,
            onUseExpertModeCheckedChange = delegate::onUseExpertModeCheckedChange,
        )

        is TriggerSetupState.NotDetected -> NotDetectedSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.NotDetected,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onEnableExpertModeClick = delegate::onEnableExpertModeClick,
            onRecordTriggerClick = delegate::onTriggerSetupRecordClick,
        )

        is TriggerSetupState.Gamepad -> GamepadTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = triggerSetupState as TriggerSetupState.Gamepad,
            onDismissRequest = delegate::onDismissTriggerSetup,
            onEnableAccessibilityServiceClick = delegate::onEnableAccessibilityServiceClick,
            onSelectButtonType = delegate::onGamepadButtonTypeSelected,
            onRecordTriggerClick = delegate::onTriggerSetupRecordClick,
            onEnableInputMethodClick = delegate::onEnableImeClick,
            onChooseInputMethodClick = delegate::onChooseImeClick,
            onUseExpertModeCheckedChange = delegate::onUseExpertModeCheckedChange,
            onEnableExpertModeClick = delegate::onEnableExpertModeClick,
        )

        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamepadTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    sheetState: SheetState,
    state: TriggerSetupState.Gamepad,
    onRecordTriggerClick: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onSelectButtonType: (TriggerSetupState.Gamepad.Type) -> Unit = { },
    onEnableExpertModeClick: () -> Unit = {},
    onEnableInputMethodClick: () -> Unit = { },
    onChooseInputMethodClick: () -> Unit = { },
    onUseExpertModeCheckedChange: (Boolean) -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_gamepad_title),
        icon = KeyMapperIcons.SportsEsports,
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
        // There is no guarantee that a gamepad can be remapped
        RemapStatusRow(
            modifier = Modifier.fillMaxWidth(),
            color = LocalCustomColorsPalette.current.orange,
            text = stringResource(R.string.trigger_setup_status_might_remap_device),
        )

        HeaderText(text = stringResource(R.string.trigger_setup_options_title))

        val buttonStates = listOf(
            TriggerSetupState.Gamepad.Type.DPAD to
                stringResource(R.string.trigger_setup_gamepad_type_dpad),
            TriggerSetupState.Gamepad.Type.SIMPLE_BUTTONS to
                stringResource(R.string.trigger_setup_gamepad_type_simple_buttons),
        )

        val selectedState = when (state) {
            is TriggerSetupState.Gamepad.Dpad -> TriggerSetupState.Gamepad.Type.DPAD

            is TriggerSetupState.Gamepad.SimpleButtons ->
                TriggerSetupState.Gamepad.Type.SIMPLE_BUTTONS
        }

        KeyMapperSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
            buttonStates = buttonStates,
            selectedState = selectedState,
            onStateSelected = onSelectButtonType,
        )

        val isUseExpertModeChecked = when (state) {
            is TriggerSetupState.Gamepad.Dpad -> false
            is TriggerSetupState.Gamepad.SimpleButtons -> state.isUseExpertModeChecked
        }

        val isUseExpertModeEnabled = when (state) {
            is TriggerSetupState.Gamepad.Dpad -> false
            is TriggerSetupState.Gamepad.SimpleButtons -> true
        }

        CheckBoxText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.trigger_setup_screen_off_option),
            isChecked = isUseExpertModeChecked,
            isEnabled = isUseExpertModeEnabled,
            onCheckedChange = onUseExpertModeCheckedChange,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick,
        )

        when (state) {
            is TriggerSetupState.Gamepad.Dpad -> {
                InputMethodRequirementRow(
                    isEnabled = state.isImeEnabled,
                    isChosen = state.isImeChosen,
                    onEnableClick = onEnableInputMethodClick,
                    onChooseClick = onChooseInputMethodClick,
                    enablingRequiresUserInput = state.enablingRequiresUserInput,
                )
            }

            is TriggerSetupState.Gamepad.SimpleButtons -> {
                ExpertModeRequirementRow(
                    modifier = Modifier.fillMaxWidth(),
                    isVisible = state.isUseExpertModeChecked,
                    expertModeStatus = state.expertModeStatus,
                    onClick = onEnableExpertModeClick,
                )
            }
        }

        if (state is TriggerSetupState.Gamepad.Dpad) {
            HeaderText(text = stringResource(R.string.trigger_setup_information_title))

            Text(
                stringResource(R.string.trigger_setup_gamepad_information_dpad),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MouseTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.Mouse,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableExpertModeClick: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_mouse_title),
        icon = Icons.Outlined.Mouse,
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

        ExpertModeRequirementRow(
            isVisible = true,
            expertModeStatus = state.expertModeStatus,
            onClick = onEnableExpertModeClick,
        )
    }
}

@Composable
private fun PowerTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.Power,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableExpertModeClick: () -> Unit = {},
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

        ExpertModeRequirementRow(
            isVisible = true,
            expertModeStatus = state.expertModeStatus,
            onClick = onEnableExpertModeClick,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_information_title))

        Text(
            stringResource(R.string.trigger_setup_power_information),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RemapStatusButton(modifier: Modifier = Modifier, remapStatus: RemapStatus) {
    when (remapStatus) {
        RemapStatus.UNSUPPORTED -> RemapStatusRow(
            modifier = modifier,
            color = MaterialTheme.colorScheme.error,
            text = stringResource(R.string.trigger_setup_status_can_not_remap),
        )

        RemapStatus.UNCERTAIN -> RemapStatusRow(
            modifier = modifier,
            color = LocalCustomColorsPalette.current.amber,
            text = stringResource(R.string.trigger_setup_status_might_remap_button),
        )

        RemapStatus.SUPPORTED -> RemapStatusRow(
            modifier = modifier,
            color = LocalCustomColorsPalette.current.green,
            text = stringResource(R.string.trigger_setup_status_remap_button_possible),
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
    onEnableExpertModeClick: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
    onUseExpertModeCheckedChange: (Boolean) -> Unit = {},
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
            text = stringResource(R.string.trigger_setup_status_remap_button_possible),
        )

        HeaderText(text = stringResource(R.string.trigger_setup_options_title))

        CheckBoxText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.trigger_setup_screen_off_option),
            isChecked = state.isUseExpertModeChecked,
            isEnabled = !state.forceExpertMode,
            onCheckedChange = onUseExpertModeCheckedChange,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick,
        )

        ExpertModeRequirementRow(
            isVisible = state.isUseExpertModeChecked,
            expertModeStatus = state.expertModeStatus,
            onClick = onEnableExpertModeClick,
        )
    }
}

@Composable
private fun NotDetectedSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.NotDetected,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableExpertModeClick: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_no_trigger_detected_title),
        icon = KeyMapperIcons.IndeterminateQuestionBox,

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
            color = LocalCustomColorsPalette.current.amber,
            text = stringResource(R.string.trigger_setup_status_might_remap_button),
        )

        HeaderText(text = stringResource(R.string.trigger_setup_options_title))

        // Must always be checked because PRO mode is always used to increase the chances
        // of detecting the button
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

        ExpertModeRequirementRow(
            isVisible = true,
            expertModeStatus = state.expertModeStatus,
            onClick = onEnableExpertModeClick,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_information_title))

        Text(
            stringResource(R.string.trigger_setup_not_detected_information),
            style = MaterialTheme.typography.bodyMedium,
        )

        val uriHandler = LocalUriHandler.current
        val helpUrl = stringResource(R.string.url_discord_server_invite)

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                uriHandler.openUri(helpUrl)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalCustomColorsPalette.current.discord,
                contentColor = LocalCustomColorsPalette.current.onDiscord,
            ),
        ) {
            Text(stringResource(R.string.trigger_setup_get_help_button))
        }
    }
}

@Composable
private fun OtherTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.Other,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableExpertModeClick: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
    onUseExpertModeCheckedChange: (Boolean) -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_other_title),
        icon = KeyMapperIcons.IndeterminateQuestionBox,

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
            color = LocalCustomColorsPalette.current.amber,
            text = stringResource(R.string.trigger_setup_status_might_remap_button),
        )

        HeaderText(text = stringResource(R.string.trigger_setup_options_title))

        CheckBoxText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.trigger_setup_screen_off_option),
            isChecked = state.isUseExpertModeChecked,
            isEnabled = !state.forceExpertMode,
            onCheckedChange = onUseExpertModeCheckedChange,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick,
        )

        ExpertModeRequirementRow(
            isVisible = state.isUseExpertModeChecked,
            expertModeStatus = state.expertModeStatus,
            onClick = onEnableExpertModeClick,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_information_title))

        Text(
            stringResource(R.string.trigger_setup_get_help_information),
            style = MaterialTheme.typography.bodyMedium,
        )

        val uriHandler = LocalUriHandler.current
        val helpUrl = stringResource(R.string.url_discord_server_invite)

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                uriHandler.openUri(helpUrl)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalCustomColorsPalette.current.discord,
                contentColor = LocalCustomColorsPalette.current.onDiscord,
            ),
        ) {
            Text(stringResource(R.string.trigger_setup_get_help_button))
        }
    }
}

@Composable
private fun KeyboardTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerSetupState.Keyboard,
    onDismissRequest: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableExpertModeClick: () -> Unit = {},
    onRecordTriggerClick: () -> Unit = {},
    onUseExpertModeCheckedChange: (Boolean) -> Unit = {},
) {
    TriggerSetupBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.trigger_setup_keyboard_title),
        icon = Icons.Rounded.Keyboard,

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
            text = stringResource(R.string.trigger_setup_status_remap_device_possible),
        )

        HeaderText(text = stringResource(R.string.trigger_setup_options_title))

        CheckBoxText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.trigger_setup_screen_off_option),
            isChecked = state.isUseExpertModeChecked,
            isEnabled = !state.forceExpertMode,
            onCheckedChange = onUseExpertModeCheckedChange,
        )

        HeaderText(text = stringResource(R.string.trigger_setup_requirements_title))

        AccessibilityServiceRequirementRow(
            isServiceEnabled = state.isAccessibilityServiceEnabled,
            onClick = onEnableAccessibilityServiceClick,
        )

        ExpertModeRequirementRow(
            isVisible = state.isUseExpertModeChecked,
            expertModeStatus = state.expertModeStatus,
            onClick = onEnableExpertModeClick,
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
            text = stringResource(R.string.trigger_setup_status_might_remap_button),
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
fun RemapStatusRow(modifier: Modifier = Modifier, color: Color, text: String) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
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
                            modifier = Modifier.size(32.dp),
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        PowerTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Power(
                isAccessibilityServiceEnabled = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
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
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        PowerTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Power(
                isAccessibilityServiceEnabled = false,
                expertModeStatus = ExpertModeStatus.UNSUPPORTED,
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
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        VolumeTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Volume(
                isAccessibilityServiceEnabled = true,
                isUseExpertModeChecked = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
                areRequirementsMet = true,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
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
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        VolumeTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Volume(
                isAccessibilityServiceEnabled = false,
                isUseExpertModeChecked = true,
                expertModeStatus = ExpertModeStatus.DISABLED,
                areRequirementsMet = false,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
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
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
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
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun KeyboardButtonEnabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        KeyboardTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Keyboard(
                isAccessibilityServiceEnabled = true,
                isUseExpertModeChecked = false,
                expertModeStatus = ExpertModeStatus.DISABLED,
                areRequirementsMet = true,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun KeyboardButtonDisabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        KeyboardTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Keyboard(
                isAccessibilityServiceEnabled = false,
                isUseExpertModeChecked = true,
                expertModeStatus = ExpertModeStatus.DISABLED,
                areRequirementsMet = false,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MouseButtonPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        MouseTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Mouse(
                isAccessibilityServiceEnabled = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
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
private fun MouseButtonDisabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        MouseTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Mouse(
                isAccessibilityServiceEnabled = false,
                expertModeStatus = ExpertModeStatus.UNSUPPORTED,
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
private fun OtherButtonPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        OtherTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Other(
                isAccessibilityServiceEnabled = true,
                isUseExpertModeChecked = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
                areRequirementsMet = true,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun OtherButtonDisabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        OtherTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Other(
                isAccessibilityServiceEnabled = false,
                isUseExpertModeChecked = true,
                expertModeStatus = ExpertModeStatus.DISABLED,
                areRequirementsMet = false,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun GamepadDpadPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        GamepadTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Gamepad.Dpad(
                isAccessibilityServiceEnabled = true,
                isImeEnabled = true,
                isImeChosen = true,
                areRequirementsMet = true,
                recordTriggerState = RecordTriggerState.Idle,
                enablingRequiresUserInput = true,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun GamepadDpadDisabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        GamepadTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Gamepad.Dpad(
                isAccessibilityServiceEnabled = false,
                isImeEnabled = false,
                isImeChosen = false,
                areRequirementsMet = false,
                recordTriggerState = RecordTriggerState.Idle,
                enablingRequiresUserInput = true,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun GamepadSimpleButtonsPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        GamepadTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Gamepad.SimpleButtons(
                isAccessibilityServiceEnabled = true,
                isUseExpertModeChecked = true,
                expertModeStatus = ExpertModeStatus.ENABLED,
                areRequirementsMet = true,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun GamepadSimpleButtonsDisabledPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        GamepadTriggerSetupBottomSheet(
            sheetState = sheetState,
            state = TriggerSetupState.Gamepad.SimpleButtons(
                isAccessibilityServiceEnabled = false,
                isUseExpertModeChecked = false,
                expertModeStatus = ExpertModeStatus.DISABLED,
                areRequirementsMet = false,
                recordTriggerState = RecordTriggerState.Idle,
                forceExpertMode = false,
            ),
        )
    }
}
