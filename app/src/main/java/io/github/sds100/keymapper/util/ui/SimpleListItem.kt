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
    icon: @Composable RowScope.() -> Unit,
    title: @Composable ColumnScope.() -> Unit,
    subtitle: @Composable ColumnScope.() -> Unit,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    OutlinedCard(modifier = modifier, onClick = onClick, enabled = enabled) {
        CardContents(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            icon = icon,
            title = title,
            subtitle = subtitle
        )
    }
}

@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    icon: @Composable RowScope.() -> Unit,
    title: @Composable ColumnScope.() -> Unit,
    subtitle: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = modifier) {
        CardContents(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            icon = icon,
            title = title,
            subtitle = subtitle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    icon: KMIcon? = null,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    OutlinedCard(modifier = modifier, onClick = onClick) {
        CardContents(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            icon = icon,
            title = title,
            subtitle = subtitle
        )
    }
}

@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    icon: KMIcon? = null,
    title: String,
    subtitle: String? = null
) {
    OutlinedCard(modifier = modifier) {
        CardContents(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            icon = icon,
            title = title,
            subtitle = subtitle
        )
    }
}

@Composable
private fun CardContents(
    modifier: Modifier = Modifier,
    icon: KMIcon? = null,
    title: String,
    subtitle: String? = null,
) {
    CardContents(modifier,
        icon = {
            if (icon != null) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically),
                    icon = icon
                )

                Spacer(Modifier.width(16.dp))
            }
        },
        title = {
            Text(title, style = MaterialTheme.typography.bodyMedium)
        },
        subtitle = {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        })
}

@Composable
private fun CardContents(
    modifier: Modifier = Modifier,
    icon: @Composable RowScope.() -> Unit,
    title: @Composable ColumnScope.() -> Unit,
    subtitle: @Composable ColumnScope.() -> Unit
) {
    Row(modifier) {
        icon()

        Column(
            Modifier
                .align(Alignment.CenterVertically)
                .defaultMinSize(minHeight = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            title()
            subtitle()
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewWithSubtitle() {
    MaterialTheme {
        SimpleListItem(
            Modifier.fillMaxWidth(),
            icon = KMIcon.ImageVector(Icons.Outlined.Face),
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
            icon = KMIcon.ImageVector(Icons.Outlined.Face),
            title = "Volume up"
        )
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewNoIcon() {
    MaterialTheme {
        SimpleListItem(
            Modifier.fillMaxWidth(),
            title = "Volume up"
        )
    }
}