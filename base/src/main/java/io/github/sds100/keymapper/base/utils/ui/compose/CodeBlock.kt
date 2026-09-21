package io.github.sds100.keymapper.base.utils.ui.compose

import android.content.ClipData
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import kotlinx.coroutines.launch

object CodeBlockDefaults {
    /**
     * Soft wrap the code after this many characters so that long commands stay readable.
     */
    const val MAX_LINE_LENGTH: Int = 80

    val MaxHeight: Dp = 240.dp
}

/**
 * Shows monospace text that can be selected, scrolled in both directions and copied.
 */
@Composable
fun CodeBlock(
    modifier: Modifier = Modifier,
    code: String,
    clipboardLabel: String,
    label: String? = null,
    maxLineLength: Int = CodeBlockDefaults.MAX_LINE_LENGTH,
    maxHeight: Dp = CodeBlockDefaults.MaxHeight,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)

    // The code only wraps once it is this wide, so narrow screens scroll horizontally
    // instead of wrapping every few words.
    val lineWidth = remember(textStyle, maxLineLength, density, textMeasurer) {
        val widthPx = textMeasurer.measure(
            text = "0".repeat(maxLineLength),
            style = textStyle,
            softWrap = false,
        ).size.width

        with(density) { widthPx.toDp() }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectionContainer(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                            .widthIn(max = lineWidth),
                        text = code,
                        style = textStyle,
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText(clipboardLabel, code)),
                            )
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = if (label == null) {
                            stringResource(R.string.code_block_copy)
                        } else {
                            stringResource(R.string.code_block_copy_content_description, label)
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CodeBlockPreview() {
    KeyMapperTheme {
        Surface {
            CodeBlock(
                modifier = Modifier.padding(16.dp),
                label = "Command",
                code = "adb shell am broadcast -n io.github.sds100.keymapper/io.github." +
                    "sds100.keymapper.api.TriggerKeyMapsBroadcastReceiver " +
                    "-a io.github.sds100.keymapper.ACTION_TRIGGER_KEYMAP_BY_UID " +
                    "--es io.github.sds100.keymapper.EXTRA_KEYMAP_UID " +
                    "beea7ef5-e33e-4bd3-9987-9002e5035f23",
                clipboardLabel = "Command",
            )
        }
    }
}

@Preview
@Composable
private fun CodeBlockShortPreview() {
    KeyMapperTheme {
        Surface {
            CodeBlock(
                modifier = Modifier.padding(16.dp),
                code = "io.github.sds100.keymapper.debug",
                clipboardLabel = "Package",
            )
        }
    }
}
