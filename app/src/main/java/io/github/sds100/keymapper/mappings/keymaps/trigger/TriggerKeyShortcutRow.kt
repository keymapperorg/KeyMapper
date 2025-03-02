package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme

@Composable
fun TriggerKeyShortcutRowNoTrigger(
    modifier: Modifier = Modifier,
    shortcuts: Set<TriggerKeyShortcut>,
    onClick: (TriggerKeyShortcut) -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            16.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        for (shortcut in shortcuts) {
            TriggerKeyShortcutButton(
                onClick = { onClick(shortcut) },
                text = when (shortcut) {
                    TriggerKeyShortcut.ASSISTANT -> stringResource(R.string.trigger_key_shortcut_use_assistant)
                    TriggerKeyShortcut.FLOATING_BUTTON -> stringResource(R.string.trigger_key_shortcut_use_floating_button)
                },
                icon = getShortcutIcon(shortcut),
            )
        }
    }
}

@Composable
private fun getShortcutIcon(shortcut: TriggerKeyShortcut) = when (shortcut) {
    TriggerKeyShortcut.ASSISTANT -> Icons.Filled.Assistant
    TriggerKeyShortcut.FLOATING_BUTTON -> Icons.Outlined.BubbleChart
}

@Composable
fun TriggerKeyShortcutRow(
    modifier: Modifier = Modifier,
    shortcuts: Set<TriggerKeyShortcut>,
    onClick: (TriggerKeyShortcut) -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            16.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.Center,
    ) {
        for (shortcut in shortcuts) {
            TriggerKeyShortcutButton(
                onClick = { onClick(shortcut) },
                text = when (shortcut) {
                    TriggerKeyShortcut.ASSISTANT -> stringResource(R.string.trigger_key_shortcut_add_assistant)
                    TriggerKeyShortcut.FLOATING_BUTTON -> stringResource(R.string.trigger_key_shortcut_add_floating_button)
                },
                icon = getShortcutIcon(shortcut),
            )
        }
    }
}

@Composable
private fun TriggerKeyShortcutButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        Surface {
            TriggerKeyShortcutRow(
                shortcuts = setOf(
                    TriggerKeyShortcut.ASSISTANT,
                    TriggerKeyShortcut.FLOATING_BUTTON,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNoTrigger() {
    KeyMapperTheme {
        Surface {
            TriggerKeyShortcutRowNoTrigger(
                shortcuts = setOf(
                    TriggerKeyShortcut.ASSISTANT,
                    TriggerKeyShortcut.FLOATING_BUTTON,
                ),
            )
        }
    }
}
