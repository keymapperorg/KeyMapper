package io.github.sds100.keymapper.mappings.keymaps

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.sds100.keymapper.KeyMapperTheme
import io.github.sds100.keymapper.R

/**
 * This row of buttons is shown at the bottom of the TriggerFragment.
 */
@Composable
fun RecordTriggerButtonRow(modifier: Modifier = Modifier) {
    Row(modifier) {
        RecordTriggerButton()
    }
}

@Composable
private fun RecordTriggerButton() {
    FilledTonalButton(
        onClick = {},
        colors = ButtonDefaults.filledTonalButtonColors()
            .copy(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(stringResource(R.string.button_record_trigger))
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        RecordTriggerButtonRow()
    }
}