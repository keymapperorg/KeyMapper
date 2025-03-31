package io.github.sds100.keymapper.groups

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R

@Composable
fun GroupBreadcrumbRow(
    modifier: Modifier = Modifier,
    groups: List<GroupListItemModel>,
    onGroupClick: (String?) -> Unit,
    enabled: Boolean = true,
) {
    Row(modifier = modifier) {
        val color = LocalContentColor.current.copy(alpha = 0.7f)
        Breadcrumb(
            text = stringResource(R.string.home_groups_breadcrumb_home),
            onClick = { onGroupClick(null) },
            color = color,
            enabled = enabled,
        )

        for ((index, group) in groups.withIndex()) {
            Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = color)

            Breadcrumb(
                text = group.name,
                onClick = { onGroupClick(group.uid) },
                color = if (index == groups.lastIndex) {
                    LocalContentColor.current
                } else {
                    color
                },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun Breadcrumb(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        Surface(
            modifier = modifier,
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            color = Color.Transparent,
            enabled = enabled,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
            )
        }
    }
}
