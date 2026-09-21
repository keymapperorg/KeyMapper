package io.github.sds100.keymapper.base.home

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.onboarding.WhatsNewItem
import io.github.sds100.keymapper.base.onboarding.WhatsNewNotes
import io.github.sds100.keymapper.base.onboarding.WhatsNewState
import io.github.sds100.keymapper.base.utils.ui.compose.CustomDialog
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcon
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.openUriSafe

@Composable
fun WhatsNewDialog(state: WhatsNewState, onDismissRequest: () -> Unit) {
    CustomDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.pos_ok))
            }
        },
        dismissButton = {},
    ) { WhatsNewDialogContent(state = state) }
}

/**
 * The scrollable body of the What's New dialog. The [footer] is shown after the changelog link.
 */
@Composable
fun WhatsNewDialogContent(
    modifier: Modifier = Modifier,
    state: WhatsNewState,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val changelogUrl = stringResource(R.string.url_changelog)

    Column {
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WhatsNewHeader(
                modifier = Modifier.padding(horizontal = 24.dp),
                versionName = state.versionName,
            )

//            state.notes.spotlight?.let {
//                WhatsNewSpotlightCard(
//                    modifier = Modifier.padding(horizontal = 24.dp),
//                    item = it,
//                )
//            }

            WhatsNewSection(
                title = stringResource(R.string.whats_new_section_new),
                icon = Icons.Rounded.AutoAwesome,
                items = state.notes.new,
            )

            WhatsNewSection(
                title = stringResource(R.string.whats_new_section_improved),
                icon = Icons.Rounded.CleaningServices,
                items = state.notes.improved,
            )

            WhatsNewSection(
                title = stringResource(R.string.whats_new_section_fixed),
                icon = Icons.Rounded.BugReport,
                items = state.notes.fixed,
            )

            TextButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Start),
                onClick = { uriHandler.openUriSafe(ctx, changelogUrl) },
            ) {
                Text(stringResource(R.string.whats_new_view_changelog_button))
            }
        }

        if (footer != null) {
            footer()
        }
    }
}

@Composable
private fun WhatsNewHeader(modifier: Modifier = Modifier, versionName: String) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            modifier = Modifier.size(48.dp),
            imageVector = KeyMapperIcons.KeyMapperIcon,
            contentDescription = null,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.whats_new),
                style = MaterialTheme.typography.headlineSmall,
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    text = stringResource(R.string.whats_new_version_badge, versionName),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun WhatsNewSection(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    items: List<WhatsNewItem>,
) {
    if (items.isEmpty()) {
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            icon = icon,
            text = title,
        )

        for (item in items) {
            if (item.isSpotlight) {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f),
                    ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    WhatsNewItemText(Modifier.padding(vertical = 8.dp, horizontal = 12.dp), item)
                }
            } else {
                WhatsNewItemText(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    item,
                )
            }
        }
    }
}

@Composable
private fun WhatsNewItemText(modifier: Modifier = Modifier, item: WhatsNewItem) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(text = item.title, style = MaterialTheme.typography.bodyMedium)

            if (item.description != null) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val previewState = WhatsNewState(
    versionName = "4.4.0",
    notes = WhatsNewNotes(
        new = listOf(
            WhatsNewItem(
                title = "Cycle keyboard language",
                description = "Switch between your keyboard's languages with a key press",
                isSpotlight = true,
            ),
            WhatsNewItem("Sort key maps by enabled/disabled", null),
            WhatsNewItem("Export/import key maps on Android TV", null),
        ),
        improved = listOf(
            WhatsNewItem("Screen tap/swipe/pinch actions now scale to display resolution", null),
        ),
        fixed = listOf(
            WhatsNewItem("Fix some crashes when auto starting Expert Mode", null),
            WhatsNewItem("Fix shell commands failing with Windows line endings", null),
        ),
    ),
)

@Preview(showSystemUi = true)
@Composable
private fun PreviewWhatsNewDialog() {
    KeyMapperTheme {
        WhatsNewDialog(state = previewState, onDismissRequest = {})
    }
}

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWhatsNewDialogDark() {
    KeyMapperTheme {
        WhatsNewDialog(state = previewState, onDismissRequest = {})
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewWhatsNewDialogNoSpotlight() {
    KeyMapperTheme {
        WhatsNewDialog(
            state = previewState.copy(
                notes = WhatsNewNotes(
                    new = emptyList(),
                    improved = emptyList(),
                    fixed = listOf(WhatsNewItem("Many bug fixes and improvements", null)),
                ),
            ),
            onDismissRequest = {},
        )
    }
}
