package io.github.sds100.keymapper.util.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CollapsableFloatingActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    text: String,
    showText: Boolean,
) {
    FloatingActionButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = text)

            AnimatedVisibility(showText) {
                AnimatedContent(text) { text ->
                    Text(modifier = Modifier.padding(start = 8.dp), text = text)
                }
            }
        }
    }
}
