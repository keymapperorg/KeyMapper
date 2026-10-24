package io.github.sds100.keymapper.base.trigger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette

@Composable
fun RemapStatusRow(modifier: Modifier = Modifier, color: Color, text: String) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = color,
                    shape = CircleShape,
                ),
        )

        Spacer(Modifier.width(16.dp))

        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun RemapStatusButton(modifier: Modifier = Modifier, remapStatus: RemapStatus) {
    when (remapStatus) {
        RemapStatus.UNSUPPORTED -> RemapStatusRow(
            modifier = modifier,
            color = MaterialTheme.colorScheme.error,
            text = stringResource(R.string.trigger_setup_status_can_not_remap),
        )

        RemapStatus.UNCERTAIN -> RemapStatusRow(
            modifier = modifier,
            color = LocalCustomColorsPalette.current.amber,
            text = stringResource(R.string.trigger_setup_status_might_remap_button),
        )

        RemapStatus.SUPPORTED -> RemapStatusRow(
            modifier = modifier,
            color = LocalCustomColorsPalette.current.green,
            text = stringResource(R.string.trigger_setup_status_remap_button_possible),
        )
    }
}

@Composable
fun TriggerRequirementsNotMetButton(modifier: Modifier = Modifier) {
    FilledTonalButton(modifier = modifier, onClick = {}, enabled = false) {
        Text(stringResource(R.string.trigger_setup_requirements_not_met_button))
    }
}

@Composable
fun AddTriggerButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(modifier = modifier, onClick = onClick) {
        Text(stringResource(R.string.trigger_setup_add_trigger_button))
    }
}
