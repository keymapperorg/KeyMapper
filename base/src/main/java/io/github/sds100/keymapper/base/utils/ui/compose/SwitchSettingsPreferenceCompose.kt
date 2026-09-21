package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The same layout as [SwitchPreferenceCompose] but with a trailing settings cog that opens
 * further configuration for the option, e.g. [io.github.sds100.keymapper.base.keymaps.TriggerVibrationBottomSheet].
 */
@Composable
fun SwitchSettingsPreferenceCompose(
    modifier: Modifier = Modifier,
    title: String,
    text: String?,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    isEnabled: Boolean = true,
    isSettingsEnabled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        onClick = {
            onCheckedChange(!isChecked)
        },
        enabled = isEnabled,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                enabled = isSettingsEnabled,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled,
            )
        }
    }
}
