package io.github.sds100.keymapper.base.trigger

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.ui.compose.icons.IndeterminateQuestionBox
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.ModeOffOn
import io.github.sds100.keymapper.base.utils.ui.compose.icons.SportsEsports
import io.github.sds100.keymapper.base.utils.ui.compose.icons.VoiceSelection

@Composable
fun TriggerDiscoverScreen(
    modifier: Modifier = Modifier,
    onShortcutClick: (TriggerSetupShortcut) -> Unit = {},
    showFingerprintGestures: Boolean = false,
    extraSections: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val horizontalPadding = 16.dp

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
            Text(
                text = stringResource(R.string.trigger_discover_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.trigger_discover_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        TriggerSection(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            title = stringResource(R.string.trigger_discover_section_on_device_buttons),
            shortcuts = buildList {
                add(
                    TriggerShortcutUiState(
                        TriggerSetupShortcut.VOLUME,
                        stringResource(R.string.trigger_discover_shortcut_volume),
                        Icons.AutoMirrored.Outlined.VolumeUp,
                    ),
                )

                add(
                    TriggerShortcutUiState(
                        TriggerSetupShortcut.ASSISTANT,
                        stringResource(R.string.trigger_discover_shortcut_assistant),
                        KeyMapperIcons.VoiceSelection,
                    ),
                )

                add(
                    TriggerShortcutUiState(
                        TriggerSetupShortcut.POWER,
                        stringResource(R.string.trigger_discover_shortcut_power),
                        KeyMapperIcons.ModeOffOn,
                    ),
                )

                if (showFingerprintGestures) {
                    add(
                        TriggerShortcutUiState(
                            TriggerSetupShortcut.FINGERPRINT_GESTURE,
                            stringResource(R.string.trigger_discover_shortcut_fingerprint_gesture),
                            Icons.Rounded.Fingerprint,
                        ),
                    )
                }
            },
            onShortcutClick = onShortcutClick,
        )

        TriggerSection(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            title = stringResource(R.string.trigger_discover_section_peripherals_gaming),
            shortcuts = listOf(
                TriggerShortcutUiState(
                    TriggerSetupShortcut.KEYBOARD,
                    stringResource(R.string.trigger_discover_shortcut_keyboard),
                    Icons.Outlined.Keyboard,
                ),
                TriggerShortcutUiState(
                    TriggerSetupShortcut.MOUSE,
                    stringResource(R.string.trigger_discover_shortcut_mouse),
                    Icons.Outlined.Mouse,
                ),
                TriggerShortcutUiState(
                    TriggerSetupShortcut.GAMEPAD,
                    stringResource(R.string.trigger_discover_shortcut_gamepad),
                    KeyMapperIcons.SportsEsports,
                ),
                TriggerShortcutUiState(
                    TriggerSetupShortcut.OTHER,
                    stringResource(R.string.trigger_discover_shortcut_other),
                    KeyMapperIcons.IndeterminateQuestionBox,
                ),
            ),
            onShortcutClick = onShortcutClick,
        )

        if (extraSections != null) {
            extraSections()
        }
    }
}

@Composable
fun <ID> TriggerSection(
    modifier: Modifier = Modifier,
    title: String,
    shortcuts: List<TriggerShortcutUiState<ID>>,
    onShortcutClick: (ID) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (shortcut in shortcuts) {
                ShortcutButton(
                    label = shortcut.label,
                    icon = shortcut.icon,
                    backgroundColor = backgroundColor,
                    onClick = { onShortcutClick(shortcut.id) },
                )
            }
        }
    }
}

@Composable
private fun ShortcutButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.widthIn(max = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            color = backgroundColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.contentColorFor(backgroundColor).takeOrElse {
                        LocalCustomColorsPalette.current.contentColorFor(backgroundColor)
                    },
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

data class TriggerShortcutUiState<ID>(val id: ID, val label: String, val icon: ImageVector)

@Preview(name = "Normal Phone")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TriggerDiscoverScreenPreview() {
    KeyMapperTheme {
        Surface {
            TriggerDiscoverScreen(
                showFingerprintGestures = true,
            )
        }
    }
}

@Preview(name = "Small Phone", widthDp = 320, heightDp = 568)
@Composable
private fun TriggerDiscoverScreenSmallPhonePreview() {
    KeyMapperTheme {
        Surface {
            TriggerDiscoverScreen()
        }
    }
}
