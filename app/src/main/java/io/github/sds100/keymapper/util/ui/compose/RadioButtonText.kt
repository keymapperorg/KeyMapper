package io.github.sds100.keymapper.util.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RadioButtonText(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    isEnabled: Boolean = true,
    maxLines: Int = 2,
    onSelected: () -> Unit,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .clickable(enabled = isEnabled, onClick = onSelected)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                enabled = isEnabled,
                // This is null so tapping on the radio button highlights the whole row.
                onClick = null,
            )

            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = text,
                style = if (isEnabled) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current.copy(
                            alpha = 0.5f,
                        ),
                    )
                },
                maxLines = maxLines,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
