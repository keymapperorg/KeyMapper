package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ProModeStatus

@Composable
fun ProModeRequirementRow(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    proModeStatus: ProModeStatus,
    buttonColors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200)),
    ) {
        SetupRequirementRow(
            modifier = modifier,
            text = stringResource(R.string.trigger_setup_pro_mode_title),
        ) {
            if (proModeStatus == ProModeStatus.UNSUPPORTED) {
                Text(
                    text = stringResource(R.string.trigger_setup_pro_mode_unsupported),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                SetupRequirementButton(
                    enabledText = stringResource(R.string.trigger_setup_pro_mode_enable_button),
                    disabledText = stringResource(R.string.trigger_setup_pro_mode_running_button),
                    isEnabled = proModeStatus != ProModeStatus.ENABLED,
                    colors = buttonColors,
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
    buttonColors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    onClick: () -> Unit,
) {
    SetupRequirementRow(
        modifier = modifier,
        text = stringResource(R.string.trigger_setup_accessibility_service_title),
    ) {
        SetupRequirementButton(
            enabledText = stringResource(R.string.trigger_setup_accessibility_service_enable_button),
            disabledText = stringResource(R.string.trigger_setup_accessibility_service_running_button),
            isEnabled = !isServiceEnabled,
            colors = buttonColors,
            onClick = onClick,
        )
    }
}

@Composable
fun InputMethodRequirementRow(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    isChosen: Boolean,
    enablingRequiresUserInput: Boolean,
    buttonColors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    onEnableClick: () -> Unit,
    onChooseClick: () -> Unit,
) {
    SetupRequirementRow(
        modifier = modifier,
        text = stringResource(R.string.trigger_setup_input_method_title),
    ) {
        val enabledText =
            when {
                !isEnabled && enablingRequiresUserInput -> stringResource(R.string.trigger_setup_input_method_enable_button)
                !isChosen -> stringResource(R.string.trigger_setup_input_method_choose_button)
                else -> ""
            }

        val disabledText = stringResource(R.string.trigger_setup_input_method_running_button)

        SetupRequirementButton(
            enabledText = enabledText,
            disabledText = disabledText,
            isEnabled = !isEnabled || !isChosen,
            colors = buttonColors,
            onClick = if (!isEnabled && enablingRequiresUserInput) onEnableClick else onChooseClick,
        )
    }
}

@Composable
fun SetupRequirementRow(
    modifier: Modifier = Modifier,
    text: String,
    actionContent: @Composable () -> Unit,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Companion.CenterVertically) {
        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.width(8.dp))

        actionContent()
    }
}

@Composable
fun SetupRequirementButton(
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
        colors = colors,
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
