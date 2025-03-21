package io.github.sds100.keymapper.mappings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyShortcut
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo

@Composable
fun <T> ShortcutRow(
    modifier: Modifier = Modifier,
    shortcuts: Set<ShortcutModel<T>>,
    onClick: (T) -> Unit = {},
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
            ShortcutButton(
                onClick = { onClick(shortcut.data) },
                text = shortcut.text,
                icon = {
                    when (shortcut.icon) {
                        is ComposeIconInfo.Drawable -> {
                            val painter = rememberDrawablePainter(shortcut.icon.drawable)
                            Icon(painter = painter, contentDescription = null)
                        }

                        is ComposeIconInfo.Vector -> {
                            Icon(imageVector = shortcut.icon.imageVector, contentDescription = null)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ShortcutButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit,
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
            icon()
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
            ShortcutRow(
                shortcuts = setOf(
                    ShortcutModel(
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Fingerprint),
                        text = stringResource(R.string.trigger_key_shortcut_add_fingerprint_gesture),
                        data = TriggerKeyShortcut.FINGERPRINT_GESTURE,
                    ),
                ),
            )
        }
    }
}
