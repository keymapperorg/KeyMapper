package io.github.sds100.keymapper.base.keymaps

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.IntentApi
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CodeBlock

@Composable
fun TriggerByIntentScreen(
    modifier: Modifier = Modifier,
    keyMapUid: String,
    onBackClick: () -> Unit,
) {
    // Use the package name of this build so the .debug and .ci builds show the correct one.
    val packageName = LocalContext.current.packageName
    val clipboardLabel = stringResource(R.string.intent_screen_clipboard_label)

    IntentScreen(
        modifier = modifier,
        title = stringResource(R.string.key_map_options_trigger_by_intent_title),
        text = stringResource(R.string.intent_screen_trigger_message),
        onBackClick = onBackClick,
    ) {
        Text(
            text = stringResource(R.string.intent_screen_target_explanation),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_package),
            code = packageName,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_class),
            code = IntentApi.TRIGGER_RECEIVER_CLASS,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_action),
            code = IntentApi.ACTION_TRIGGER_KEYMAP_BY_UID,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_extra_name),
            code = EXTRA_KEYMAP_UID,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_extra_value),
            code = keyMapUid,
            clipboardLabel = clipboardLabel,
        )

        AdbSection(
            command = buildAdbCommand(
                packageName = packageName,
                receiverClass = IntentApi.TRIGGER_RECEIVER_CLASS,
                action = IntentApi.ACTION_TRIGGER_KEYMAP_BY_UID,
                keyMapUid = keyMapUid,
            ),
        )
    }
}

@Preview
@Composable
private fun TriggerByIntentScreenPreview() {
    KeyMapperTheme {
        TriggerByIntentScreen(
            keyMapUid = "beea7ef5-e33e-4bd3-9987-9002e5035f23",
            onBackClick = {},
        )
    }
}
