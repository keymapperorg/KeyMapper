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
fun PauseResumeByIntentScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit) {
    val packageName = LocalContext.current.packageName
    val clipboardLabel = stringResource(R.string.intent_screen_clipboard_label)

    IntentScreen(
        modifier = modifier,
        title = stringResource(R.string.title_pref_pause_resume_by_intent),
        text = stringResource(R.string.intent_screen_pause_resume_message),
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
            code = IntentApi.PAUSE_RECEIVER_CLASS,
            clipboardLabel = clipboardLabel,
        )

        Text(
            text = stringResource(R.string.intent_screen_action_explanation),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_tab_pause),
            code = IntentApi.ACTION_PAUSE_MAPPINGS,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_tab_resume),
            code = IntentApi.ACTION_RESUME_MAPPINGS,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_tab_toggle),
            code = IntentApi.ACTION_TOGGLE_MAPPINGS,
            clipboardLabel = clipboardLabel,
        )

        AdbSection(
            command = buildAdbCommand(
                packageName = packageName,
                receiverClass = IntentApi.PAUSE_RECEIVER_CLASS,
                action = IntentApi.ACTION_TOGGLE_MAPPINGS,
            ),
        )
    }
}

@Preview(heightDp = 1000)
@Composable
private fun PauseResumeByIntentScreenPreview() {
    KeyMapperTheme {
        PauseResumeByIntentScreen(onBackClick = {})
    }
}
