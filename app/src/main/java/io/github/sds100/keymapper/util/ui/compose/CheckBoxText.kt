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
fun CheckBoxText(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean,
    isEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .clickable(enabled = isEnabled) { onCheckedChange(!isChecked) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                enabled = isEnabled,
                checked = isChecked,
                // This is null so tapping on the checkbox highlights the whole row.
                onCheckedChange = null,
            )

            Text(
                modifier = Modifier.padding(horizontal = 8.dp),

                text = text,
                style = if (isEnabled) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.surfaceVariant)
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
