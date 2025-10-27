package io.github.sds100.keymapper.base.groups

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R

@Composable
fun GroupBreadcrumbRow(
    modifier: Modifier = Modifier,
    groups: List<GroupListItemModel>,
    onGroupClick: (String?) -> Unit,
    enabled: Boolean = true,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(groups) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    BoxWithConstraints(modifier = modifier) {
        val maxCrumbWidth = constraints.maxWidth / 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),

        ) {
            val color = LocalContentColor.current.copy(alpha = 0.7f)
            Breadcrumb(
                text = stringResource(R.string.home_groups_breadcrumb_home),
                onClick = { onGroupClick(null) },
                color = color,
                enabled = enabled,
            )

            for ((index, group) in groups.withIndex()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    null,
                    tint = color,
                )

                Breadcrumb(
                    modifier = Modifier.widthIn(
                        max = LocalDensity.current.run {
                            maxCrumbWidth.toDp()
                        },
                    ),
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
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
