package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Created by sds100 on 30/07/2022.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckBoxWithText(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(modifier.clickable { onCheckedChange(!isChecked) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Text(text = text)
    }
}

@Preview(widthDp = 400)
@Composable
private fun Preview() {
    MaterialTheme {
        RadioButtonWithText(isSelected = true, text = "Radio button")
    }
}