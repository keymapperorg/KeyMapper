package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.home.chipHeight

@Composable
fun CompactChip(
    modifier: Modifier = Modifier,
    text: String,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = false,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        if (onClick == null || !enabled) {
            Surface(
                modifier = modifier.height(chipHeight),
                color = containerColor,
                shape = AssistChipDefaults.shape,
            ) {
                CompactChipContent(icon, text, contentColor)
            }
        } else {
            Surface(
                modifier = modifier.height(chipHeight),
                color = containerColor,
                shape = AssistChipDefaults.shape,
                onClick = onClick,
            ) {
                CompactChipContent(icon, text, contentColor)
            }
        }
    }
}

@Composable
fun ErrorCompactChip(
    onClick: () -> Unit,
    text: String,
    enabled: Boolean,
) {
    CompactChip(
        text = text,
        icon = {
            Icon(
                modifier = Modifier.fillMaxHeight(),
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
            )
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun CompactChipContent(
    icon: @Composable (() -> Unit)?,
    text: String,
    contentColor: Color,
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(4.dp))
        }

        Text(
            text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
