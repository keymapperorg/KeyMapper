package io.github.sds100.keymapper.base.keymaps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.drawable

@Composable
fun <T> ShortcutRow(
    modifier: Modifier = Modifier,
    shortcuts: Set<ShortcutModel<T>>,
    onClick: (T) -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp,
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
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painter,
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }

                        is ComposeIconInfo.Vector -> {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = shortcut.icon.imageVector,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun ShortcutButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewVector() {
    KeyMapperTheme {
        Surface {
            ShortcutRow(
                shortcuts =
                    setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.Fingerprint),
                            text = stringResource(R.string.trigger_key_shortcut_add_fingerprint_gesture),
                            data = "",
                        ),
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDrawable() {
    val ctx = LocalContext.current
    val icon = ctx.drawable(R.mipmap.ic_launcher_round)

    KeyMapperTheme {
        Surface {
            ShortcutRow(
                shortcuts =
                    setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Drawable(icon),
                            text = stringResource(R.string.trigger_key_shortcut_add_fingerprint_gesture),
                            data = "",
                        ),
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewMultipleLines() {
    val ctx = LocalContext.current
    val icon = ctx.drawable(R.mipmap.ic_launcher_round)

    KeyMapperTheme {
        Surface {
            ShortcutRow(
                shortcuts =
                    setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Drawable(icon),
                            text = "Line 1\nLine 2\nLine 3",
                            data = "",
                        ),
                    ),
            )
        }
    }
}
