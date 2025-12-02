package io.github.sds100.keymapper.base.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.openUriSafe

@Composable
fun HandleAccessibilityServiceDialogs(delegate: SetupAccessibilityServiceDelegateImpl) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    when (val dialog = delegate.dialogState) {
        is AccessibilityServiceDialog.EnableService -> {
            EnableAccessibilityServiceDialog(
                modifier = Modifier,
                isRestrictedSetting = dialog.isRestrictedSetting,
                onDismissRequest = delegate::onCancelClick,
                onEnableClick = delegate::onStartServiceClick,
            )
        }

        is AccessibilityServiceDialog.RestartService -> {
            val dontKillMyAppUrl = stringResource(R.string.url_dont_kill_my_app)
            RestartAccessibilityServiceDialog(
                modifier = Modifier,
                onDismissRequest = delegate::onCancelClick,
                onRestartClick = delegate::onRestartServiceClick,
                onDontKillMyAppClick = {
                    uriHandler.openUriSafe(context, dontKillMyAppUrl)
                },
                onIgnoreClick = delegate::onIgnoreCrashedClick,
            )
        }

        is AccessibilityServiceDialog.CantFindSettings -> {
            val adbGuideUrl = stringResource(R.string.url_cant_find_accessibility_settings_issue)
            CantFindAccessibilitySettingsDialog(
                modifier = Modifier,
                onDismissRequest = delegate::onCancelClick,
                onOpenGuide = {
                    uriHandler.openUriSafe(context, adbGuideUrl)
                    delegate.onCancelClick()
                },
            )
        }

        null -> {}
    }
}

@Composable
private fun EnableAccessibilityServiceDialog(
    modifier: Modifier = Modifier,
    isRestrictedSetting: Boolean,
    onDismissRequest: () -> Unit,
    onEnableClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.dialog_title_accessibility_service_explanation))
        },
        text = {
            Column {
                if (isRestrictedSetting) {
                    RestrictedSettingText()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    stringResource(R.string.dialog_message_accessibility_service_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEnableClick) {
                Text(stringResource(R.string.enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
    )
}

@Composable
private fun RestartAccessibilityServiceDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onRestartClick: () -> Unit,
    onDontKillMyAppClick: () -> Unit,
    onIgnoreClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.dialog_title_key_mapper_crashed))
        },
        text = {
            Text(
                stringResource(R.string.dialog_message_key_mapper_crashed),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onRestartClick) {
                    Text(stringResource(R.string.pos_restart))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDontKillMyAppClick) {
                    Text(stringResource(R.string.dialog_button_read_dont_kill_my_app_yes))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onIgnoreClick) {
                Text(stringResource(R.string.dialog_button_read_dont_kill_my_app_no))
            }
        },
    )
}

@Composable
private fun CantFindAccessibilitySettingsDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.dialog_title_cant_find_accessibility_settings_page))
        },
        text = {
            Text(
                stringResource(R.string.dialog_message_cant_find_accessibility_settings_page),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenGuide) {
                Text(stringResource(R.string.pos_start_service_with_pro_mode))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
    )
}

@Composable
private fun RestrictedSettingText(modifier: Modifier = Modifier) {
    val messageText = stringResource(R.string.dialog_restricted_setting_message)
    val linkText = stringResource(R.string.dialog_restricted_setting_link_text)
    val restrictedSettingUrl = stringResource(R.string.url_restricted_setting)

    val annotatedString = buildAnnotatedString {
        append(messageText)
        append(" ")

        pushLink(LinkAnnotation.Url(restrictedSettingUrl))
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(linkText)
        }
        pop()
    }

    Text(
        modifier = modifier,
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
    )
}

@Preview
@Composable
private fun EnableAccessibilityServiceDialogPreview() {
    KeyMapperTheme {
        EnableAccessibilityServiceDialog(
            isRestrictedSetting = false,
            onDismissRequest = {},
            onEnableClick = {},
        )
    }
}

@Preview
@Composable
private fun EnableAccessibilityServiceDialogRestrictedPreview() {
    KeyMapperTheme {
        EnableAccessibilityServiceDialog(
            isRestrictedSetting = true,
            onDismissRequest = {},
            onEnableClick = {},
        )
    }
}

@Preview
@Composable
private fun RestartAccessibilityServiceDialogPreview() {
    KeyMapperTheme {
        RestartAccessibilityServiceDialog(
            onDismissRequest = {},
            onRestartClick = {},
            onDontKillMyAppClick = {},
            onIgnoreClick = {},
        )
    }
}

@Preview
@Composable
private fun CantFindAccessibilitySettingsDialogPreview() {
    KeyMapperTheme {
        CantFindAccessibilitySettingsDialog(
            onDismissRequest = {},
            onOpenGuide = {},
        )
    }
}
