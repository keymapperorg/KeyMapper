package io.github.sds100.keymapper.util.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CheckBoxTextRow(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean,
    isEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = Color.Transparent) {
        Row(
            modifier = Modifier.clickable(enabled = isEnabled) { onCheckedChange(!isChecked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                enabled = isEnabled,
                checked = isChecked,
                onCheckedChange = onCheckedChange,
            )

            Text(
                modifier = Modifier.padding(end = 8.dp),

                text = text,
                style = if (isEnabled) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.surfaceVariant)
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
