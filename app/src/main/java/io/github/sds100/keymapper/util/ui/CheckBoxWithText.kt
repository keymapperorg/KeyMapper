package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Created by sds100 on 30/07/2022.
 */

@Composable
fun CheckBoxWithText(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    text: @Composable () -> Unit,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(modifier.clickable { onCheckedChange(!isChecked) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        text()
    }
}

@Preview(widthDp = 400)
@Composable
private fun Preview() {
    MaterialTheme {
        Surface {
            CheckBoxWithText(isChecked = true, text = { Text("Radio button") })
        }
    }
}