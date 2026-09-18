package io.github.sds100.keymapper.base.keymaps

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CodeBlock

// Duplicated from the :api module because :api depends on :base and not the other
// way around. CreateKeyMapShortcutUseCase duplicates them in the same way.
private const val ACTION_ENABLE_KEY_MAP = "io.github.sds100.keymapper.ACTION_ENABLE_KEY_MAP"
private const val ACTION_DISABLE_KEY_MAP = "io.github.sds100.keymapper.ACTION_DISABLE_KEY_MAP"
private const val ACTION_TOGGLE_KEY_MAP = "io.github.sds100.keymapper.ACTION_TOGGLE_KEY_MAP"
private const val ENABLE_RECEIVER_CLASS =
    "io.github.sds100.keymapper.api.EnableKeyMapsBroadcastReceiver"

@Composable
fun EnableByIntentScreen(
    modifier: Modifier = Modifier,
    keyMapUid: String,
    onBackClick: () -> Unit,
) {
    val packageName = LocalContext.current.packageName
    val clipboardLabel = stringResource(R.string.intent_screen_clipboard_label)

    IntentScreen(
        modifier = modifier,
        title = stringResource(R.string.key_map_options_enable_by_intent_title),
        text = stringResource(R.string.intent_screen_enable_message),
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
            code = ENABLE_RECEIVER_CLASS,
            clipboardLabel = clipboardLabel,
        )

        Text(
            text = stringResource(R.string.intent_screen_action_explanation),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_action_enable),
            code = ACTION_ENABLE_KEY_MAP,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_action_disable),
            code = ACTION_DISABLE_KEY_MAP,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_action_toggle),
            code = ACTION_TOGGLE_KEY_MAP,
            clipboardLabel = clipboardLabel,
        )

        Text(
            text = stringResource(R.string.intent_screen_extras_explanation),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
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
                receiverClass = ENABLE_RECEIVER_CLASS,
                action = ACTION_TOGGLE_KEY_MAP,
                keyMapUid = keyMapUid,
            ),
        )
    }
}

@Preview(heightDp = 1200)
@Composable
private fun EnableByIntentScreenPreview() {
    KeyMapperTheme {
        EnableByIntentScreen(
            keyMapUid = "beea7ef5-e33e-4bd3-9987-9002e5035f23",
            onBackClick = {},
        )
    }
}
