package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Created by sds100 on 13/07/2022.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    icon: Icon,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    OutlinedCard(modifier = modifier, onClick = onClick) {
        Row(Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)) {
            Icon(
                icon = icon, modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically)
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.align(Alignment.CenterVertically)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)

                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewWithSubtitle() {
    MaterialTheme {
        SimpleListItem(
            Modifier.fillMaxWidth(),
            icon = Icon.ImageVector(Icons.Outlined.Face),
            title = "Volume up",
            subtitle = "Root required"
        )
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewWithoutSubtitle() {
    MaterialTheme {
        SimpleListItem(
            Modifier.fillMaxWidth(),
            icon = Icon.ImageVector(Icons.Outlined.Face),
            title = "Volume up"
        )
    }
}