package io.github.sds100.keymapper.groups

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme

@Composable
fun GroupRow(
    modifier: Modifier = Modifier,
    groups: List<SubGroupListModel>,
    onNewGroupClick: () -> Unit = {},
    onGroupClick: (String) -> Unit = {},
) {
    FlowRow(modifier) {
        AnimatedContent(groups.isEmpty()) { isEmpty ->
            GroupButton(
                onClick = onNewGroupClick,
                text = stringResource(R.string.home_new_group_button),
                icon = {
                    Icon(imageVector = Icons.Rounded.Add, null)
                },
                showText = isEmpty,
            )
        }
        // TODO only show max 2 rows, otherwise show View All.
    }
}

@Composable
private fun GroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit,
    showText: Boolean = true,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        GroupRow(groups = emptyList())
    }
}
