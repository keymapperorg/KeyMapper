package io.github.sds100.keymapper.base.trigger

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.BubbleChart
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TriggerDiscoverScreen(
    modifier: Modifier = Modifier,
    onShortcutClick: (TriggerSetupShortcut) -> Unit = {},
    showFloatingButtons: Boolean = false,
    showFingerprintGestures: Boolean = false,
) {
    val customColors = LocalCustomColorsPalette.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
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
            title = stringResource(R.string.trigger_discover_section_on_device_buttons),
            shortcuts = buildList {
                add(
                    ShortcutData(
                        TriggerSetupShortcut.VOLUME,
                        stringResource(R.string.trigger_discover_shortcut_volume),
                        Icons.AutoMirrored.Outlined.VolumeUp,
                    ),
                )

                add(
                    ShortcutData(
                        TriggerSetupShortcut.ASSISTANT,
                        stringResource(R.string.trigger_discover_shortcut_assistant),
                        KeyMapperIcons.VoiceSelection,
                    ),
                )

                add(
                    ShortcutData(
                        TriggerSetupShortcut.POWER,
                        stringResource(R.string.trigger_discover_shortcut_power),
                        KeyMapperIcons.ModeOffOn,
                    ),
                )

                if (showFingerprintGestures) {
                    add(
                        ShortcutData(
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
            title = stringResource(R.string.trigger_discover_section_peripherals_gaming),
            shortcuts = listOf(
                ShortcutData(
                    TriggerSetupShortcut.KEYBOARD,
                    stringResource(R.string.trigger_discover_shortcut_keyboard),
                    Icons.Outlined.Keyboard,
                ),
                ShortcutData(
                    TriggerSetupShortcut.MOUSE,
                    stringResource(R.string.trigger_discover_shortcut_mouse),
                    Icons.Outlined.Mouse,
                ),
                ShortcutData(
                    TriggerSetupShortcut.GAMEPAD,
                    stringResource(R.string.trigger_discover_shortcut_gamepad),
                    KeyMapperIcons.SportsEsports,
                ),
                ShortcutData(
                    TriggerSetupShortcut.OTHER,
                    stringResource(R.string.trigger_discover_shortcut_other),
                    KeyMapperIcons.IndeterminateQuestionBox,
                ),
            ),
            onShortcutClick = onShortcutClick,
        )

        AnimatedVisibility(visible = showFloatingButtons) {
            TriggerSection(
                title = stringResource(R.string.trigger_discover_section_floating_buttons),
                shortcuts = listOf(
                    ShortcutData(
                        TriggerSetupShortcut.FLOATING_BUTTON_CUSTOM,
                        stringResource(R.string.trigger_discover_shortcut_custom),
                        Icons.Outlined.BubbleChart,
                    ),
//                ShortcutData(
//                    TriggerSetupShortcut.NOTCH,
//                    stringResource(R.string.trigger_discover_shortcut_notch),
//                    Icons.Default.TouchApp
//                ),
                    ShortcutData(
                        TriggerSetupShortcut.FLOATING_BUTTON_LOCK_SCREEN,
                        stringResource(R.string.trigger_discover_shortcut_lock_screen),
                        Icons.Default.PhoneAndroid,
                    ),
                ),
                onShortcutClick = onShortcutClick,
                backgroundColor = customColors.amberContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriggerSection(
    title: String,
    shortcuts: List<ShortcutData>,
    onShortcutClick: (TriggerSetupShortcut) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Column(
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
                    shortcut = shortcut,
                    backgroundColor = backgroundColor,
                    onClick = { onShortcutClick(shortcut.type) },
                )
            }
        }
    }
}

@Composable
private fun ShortcutButton(
    modifier: Modifier = Modifier,
    shortcut: ShortcutData,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.widthIn(max = 56.dp),
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
                    imageVector = shortcut.icon,
                    contentDescription = shortcut.label,
                    tint = MaterialTheme.colorScheme.contentColorFor(backgroundColor).takeOrElse {
                        LocalCustomColorsPalette.current.contentColorFor(backgroundColor)
                    },
                )
            }
        }

        Text(
            text = shortcut.label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class ShortcutData(
    val type: TriggerSetupShortcut,
    val label: String,
    val icon: ImageVector,
)

@Preview(name = "Normal Phone")
@Composable
private fun TriggerDiscoverScreenPreview() {
    KeyMapperTheme {
        Surface {
            TriggerDiscoverScreen()
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

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TriggerDiscoverScreenDarkModePreview() {
    KeyMapperTheme {
        Surface {
            TriggerDiscoverScreen()
        }
    }
}
