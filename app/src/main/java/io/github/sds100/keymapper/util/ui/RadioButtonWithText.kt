package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Created by sds100 on 30/07/2022.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioButtonWithText(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    text: String,
    onClick: () -> Unit = {}
) {
    Row(modifier.clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(text = text)
        Spacer(Modifier.width(8.dp))
    }
}

@Preview(widthDp = 400)
@Composable
private fun Preview() {
    MaterialTheme {
        RadioButtonWithText(isSelected = true, text = "Radio button")
    }
}