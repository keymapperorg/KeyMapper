package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme

@Composable
fun OptionButton(modifier: Modifier = Modifier, title: String, text: String, onClick: () -> Unit) {
    Surface(modifier = modifier, onClick = onClick, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        OptionButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.title_pref_expert_mode),
            text = stringResource(R.string.summary_pref_expert_mode),
            onClick = {},
        )
    }
}
