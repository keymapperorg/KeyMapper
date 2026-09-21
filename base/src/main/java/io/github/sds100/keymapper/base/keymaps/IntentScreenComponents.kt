package io.github.sds100.keymapper.base.keymaps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.compose.CodeBlock

// Duplicated from the :api module because :api depends on :base and not the other
// way around. CreateKeyMapShortcutUseCase duplicates them in the same way.
internal const val EXTRA_KEYMAP_UID = "io.github.sds100.keymapper.EXTRA_KEYMAP_UID"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IntentScreen(
    modifier: Modifier = Modifier,
    title: String,
    text: String,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_go_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = innerPadding.calculateStartPadding(layoutDirection)
        val endPadding = innerPadding.calculateEndPadding(layoutDirection)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = startPadding,
                    end = endPadding,
                ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                content()
            }
        }
    }
}

@Composable
internal fun AdbSection(modifier: Modifier = Modifier, command: String) {
    val clipboardLabel = stringResource(R.string.intent_screen_clipboard_label)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.intent_screen_adb_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.intent_screen_adb_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_command),
            code = command,
            clipboardLabel = clipboardLabel,
        )
    }
}

internal fun buildAdbCommand(
    packageName: String,
    receiverClass: String,
    action: String,
    keyMapUid: String,
): String {
    return "adb shell am broadcast -n $packageName/$receiverClass " +
        "-a $action --es $EXTRA_KEYMAP_UID $keyMapUid"
}
