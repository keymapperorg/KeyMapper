package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.TextListItem

@Composable
fun ListItemFixError(
    modifier: Modifier = Modifier,
    model: TextListItem.Error,
    onFixClick: () -> Unit = {},
) {
    Row(modifier = modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = model.text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onFixClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(text = model.customButtonText ?: stringResource(R.string.button_fix))
        }
    }
}

@Preview
@Composable
private fun PreviewListItemFixError() {
    KeyMapperTheme {
        Surface {
            ListItemFixError(
                model = TextListItem.Error(
                    id = "error",
                    text = stringResource(R.string.trigger_error_dnd_access_denied),
                ),
            )
        }
    }
}
