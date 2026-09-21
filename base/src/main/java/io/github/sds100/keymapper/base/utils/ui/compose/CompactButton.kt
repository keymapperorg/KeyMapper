package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.compose.KeyMapperTheme

private val compactButtonHeight = 28.dp
private val compactButtonContentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)

@Composable
fun CompactOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke = ButtonDefaults.outlinedButtonBorder(),
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        modifier = modifier.height(compactButtonHeight),
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        contentPadding = compactButtonContentPadding,
        border = border,
        content = content,
    )
}

@Composable
fun CompactFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        modifier = modifier.height(compactButtonHeight),
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        contentPadding = compactButtonContentPadding,
        content = content,
    )
}

@Composable
fun CompactErrorButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        modifier = modifier.height(compactButtonHeight),
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        contentPadding = compactButtonContentPadding,
        content = content,
    )
}

@Composable
private fun CompactButtonsPreviewContent() {
    Surface {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactOutlinedButton(onClick = {}) {
                Text("Outlined")
            }

            CompactFilledTonalButton(onClick = {}) {
                Text("Tonal")
            }

            CompactErrorButton(onClick = {}) {
                Text("Fix")
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewLight() {
    KeyMapperTheme {
        CompactButtonsPreviewContent()
    }
}
