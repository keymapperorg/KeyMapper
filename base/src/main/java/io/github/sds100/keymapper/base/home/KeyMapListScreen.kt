package io.github.sds100.keymapper.base.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.trigger.TriggerError
import io.github.sds100.keymapper.base.utils.ui.compose.CompactChip
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.ErrorCompactChip
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.State

@Composable
fun KeyMapList(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    listItems: State<List<KeyMapListItemModel>>,
    header: (@Composable () -> Unit)? = null,
    footerText: String? = stringResource(R.string.home_key_map_list_footer_text),
    isSelectable: Boolean = false,
    onClickKeyMap: (String) -> Unit = {},
    onLongClickKeyMap: (String) -> Unit = {},
    onSelectedChange: (String, Boolean) -> Unit = { _, _ -> },
    onFixClick: (KMError) -> Unit = {},
    onTriggerErrorClick: (TriggerError) -> Unit = {},
    bottomListPadding: Dp = 100.dp,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemSpacing = 8.dp
    // The header is flush with the app bar because it is the same color as it.
    val topPadding = if (header == null) 8.dp else 0.dp
    val bottomPadding = 8.dp

    // The loading and empty states must fill the space left over by the header, like they would
    // with a weight of 1f in a Column, so the header is measured.
    var headerHeight by remember { mutableStateOf(0.dp) }

    Surface(modifier = modifier) {
        BoxWithConstraints {
            val remainingHeight = (
                maxHeight - headerHeight - topPadding - bottomListPadding - bottomPadding -
                    if (header == null) 0.dp else itemSpacing
                ).coerceAtLeast(0.dp)

            // Wait for the header to be measured so the loading and empty states are not laid out
            // a header too tall on the first frame and then jump into place.
            val isRemainingHeightMeasured = header == null || headerHeight > 0.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                // The header is in the list rather than the app bar so that it scrolls away and
                // does not take up vertical space on small screens or in landscape.
                if (header != null) {
                    item(key = "header") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged {
                                    headerHeight = with(density) { it.height.toDp() }
                                },
                        ) {
                            header()
                        }
                    }
                }

                when (listItems) {
                    is State.Loading -> {
                        if (isRemainingHeightMeasured) {
                            item(key = "loading") {
                                LoadingList(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(remainingHeight),
                                )
                            }
                        }
                    }

                    is State.Data -> {
                        if (listItems.data.isEmpty()) {
                            if (isRemainingHeightMeasured) {
                                item(key = "empty") {
                                    EmptyKeyMapList(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(remainingHeight),
                                    )
                                }
                            }
                        } else {
                            items(listItems.data, key = { it.uid }) { model ->
                                KeyMapListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    isSelectable = isSelectable,
                                    model = model,
                                    onClickKeyMap = { onClickKeyMap(model.content.uid) },
                                    onLongClickKeyMap = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongClickKeyMap(model.content.uid)
                                    },
                                    onSelectedChange = { onSelectedChange(model.content.uid, it) },
                                    onFixClick = onFixClick,
                                    onTriggerErrorClick = onTriggerErrorClick,
                                )
                            }

                            if (footerText != null) {
                                item(key = "footer") {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        text = footerText,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }

                            // Give some space at the end of the list so that the FAB doesn't block
                            // the items.
                            item(key = "bottom_padding") {
                                Spacer(Modifier.height(bottomListPadding))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingList(modifier: Modifier = Modifier) {
    Box(modifier) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun EmptyKeyMapList(modifier: Modifier = Modifier) {
    Box(modifier) {
        val shrug = stringResource(R.string.shrug)
        val text = stringResource(R.string.home_key_map_list_empty)
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
            text = buildAnnotatedString {
                withStyle(MaterialTheme.typography.headlineLarge.toSpanStyle()) {
                    append(shrug)
                }
                appendLine()
                appendLine()
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle()) {
                    append(text)
                }
            },
            textAlign = TextAlign.Center,
        )
    }
}

val chipHeight = 28.dp

@Composable
private fun KeyMapListItem(
    modifier: Modifier = Modifier,
    isSelectable: Boolean,
    model: KeyMapListItemModel,
    onClickKeyMap: () -> Unit,
    onLongClickKeyMap: () -> Unit,
    onSelectedChange: (Boolean) -> Unit,
    onFixClick: (KMError) -> Unit,
    onTriggerErrorClick: (TriggerError) -> Unit,
) {
    val cardColors = if (model.content.isEnabled) {
        CardDefaults.outlinedCardColors()
    } else {
        CardDefaults.outlinedCardColors(
            contentColor =
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }

    OutlinedCard(
        modifier = modifier,
        colors = cardColors,
        onClick = onClickKeyMap,
    ) {
        Row(
            modifier = Modifier.combinedClickable(
                onClick = onClickKeyMap,
                onLongClick = onLongClickKeyMap,
            ),
        ) {
            if (isSelectable) {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides 16.dp,
                ) {
                    Checkbox(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .align(Alignment.CenterVertically),
                        checked = model.isSelected,
                        onCheckedChange = onSelectedChange,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier.heightIn(min = chipHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TriggerDescription(
                        modifier = Modifier.fillMaxWidth(),
                        triggerKeys = model.content.triggerKeys,
                        separator = model.content.triggerSeparatorIcon,
                    )
                }

                if (model.content.triggerErrors.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            4.dp,
                            alignment = Alignment.CenterVertically,
                        ),
                    ) {
                        for (error in model.content.triggerErrors) {
                            ErrorCompactChip(
                                onClick = { onTriggerErrorClick(error) },
                                text = getTriggerErrorMessage(error),
                                enabled = error.isFixable,
                            )
                        }
                    }
                }

                if (model.content.actions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            alignment = Alignment.CenterVertically,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.action_list_header),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        for (chipModel in model.content.actions) {
                            ActionConstraintChip(
                                chipModel,
                                onFixClick = onFixClick,
                            )
                        }
                    }
                }

                if (model.content.constraints.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            alignment = Alignment.CenterVertically,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.constraint_list_header),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        for ((index, chipModel) in model.content.constraints.withIndex()) {
                            ActionConstraintChip(
                                chipModel,
                                onFixClick = onFixClick,
                            )

                            if (index < model.content.constraints.lastIndex) {
                                when (model.content.constraintMode) {
                                    ConstraintMode.AND -> Text(
                                        text = stringResource(R.string.constraint_mode_and),
                                        style = MaterialTheme.typography.labelMedium,
                                    )

                                    ConstraintMode.OR -> Text(
                                        text = stringResource(R.string.constraint_mode_or),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                if (model.content.options.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.heightIn(min = chipHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OptionsDescription(
                            modifier = Modifier.fillMaxWidth(),
                            options = model.content.options,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerDescription(
    modifier: Modifier = Modifier,
    triggerKeys: List<String>,
    separator: ImageVector,
) {
    val text = buildAnnotatedString {
        pushStyle(
            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).toSpanStyle(),
        )
        append(stringResource(R.string.trigger_header))
        pop()
        append(" ")

        if (triggerKeys.isEmpty()) {
            append(stringResource(R.string.trigger_header_none))
        } else {
            for ((index, key) in triggerKeys.withIndex()) {
                append(key)

                if (index < triggerKeys.lastIndex) {
                    append(" ")
                    appendInlineContent("separator")
                    append(" ")
                }
            }
        }
    }

    val inlineContent = mapOf(
        "separator" to InlineTextContent(
            placeholder = Placeholder(
                width = 14.sp,
                height = 14.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            Icon(imageVector = separator, contentDescription = null)
        },
    )

    Text(
        modifier = modifier,
        text = text,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun OptionsDescription(modifier: Modifier = Modifier, options: List<String>) {
    val dot = stringResource(R.string.middot)
    val text = buildAnnotatedString {
        pushStyle(
            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).toSpanStyle(),
        )
        append(stringResource(R.string.option_list_header))
        pop()
        append(" ")

        for ((index, option) in options.withIndex()) {
            append(option)

            if (index < options.lastIndex) {
                append(" $dot ")
            }
        }
    }

    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ActionConstraintChip(model: ComposeChipModel, onFixClick: (KMError) -> Unit) {
    when (model) {
        is ComposeChipModel.Normal -> {
            // Grey out disabled chips using the Material 3 alpha for disabled content.
            val contentAlpha = if (model.isEnabled) 1f else 0.38f

            CompactChip(
                text = model.text,
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                icon = model.icon?.let { icon ->
                    {
                        when (icon) {
                            is ComposeIconInfo.Drawable -> Icon(
                                modifier = Modifier
                                    .alpha(contentAlpha)
                                    .fillMaxHeight(),
                                painter = rememberDrawablePainter(icon.drawable),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )

                            is ComposeIconInfo.Vector -> Icon(
                                modifier = Modifier
                                    .fillMaxHeight(),
                                imageVector = icon.imageVector,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        }

        is ComposeChipModel.Error -> ErrorCompactChip(
            onClick = { onFixClick(model.error) },
            model.text,
            model.isFixable,
        )
    }
}

@Composable
private fun getTriggerErrorMessage(error: TriggerError): String {
    return when (error) {
        TriggerError.DND_ACCESS_DENIED -> stringResource(R.string.trigger_error_dnd_access_denied)

        TriggerError.CANT_DETECT_IN_PHONE_CALL -> stringResource(
            R.string.trigger_error_cant_detect_in_phone_call,
        )

        TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> stringResource(
            R.string.trigger_error_assistant_not_purchased,
        )

        TriggerError.DPAD_IME_NOT_SELECTED -> stringResource(
            R.string.trigger_error_dpad_ime_not_selected,
        )

        TriggerError.FLOATING_BUTTON_DELETED -> stringResource(
            R.string.trigger_error_floating_button_deleted,
        )

        TriggerError.FLOATING_BUTTONS_NOT_PURCHASED -> stringResource(
            R.string.trigger_error_floating_buttons_not_purchased,
        )

        TriggerError.PURCHASE_VERIFICATION_FAILED -> stringResource(
            R.string.trigger_error_product_verification_failed,
        )

        TriggerError.SYSTEM_BRIDGE_UNSUPPORTED -> stringResource(
            R.string.trigger_error_system_bridge_unsupported,
        )

        TriggerError.SYSTEM_BRIDGE_DISCONNECTED -> stringResource(
            R.string.trigger_error_system_bridge_disconnected,
        )

        TriggerError.EVDEV_DEVICE_NOT_FOUND -> stringResource(
            R.string.trigger_error_evdev_device_not_found,
        )

        TriggerError.MIGRATE_SCREEN_OFF_TRIGGER -> stringResource(
            R.string.trigger_error_migrate_screen_off_key_map,
        )
    }
}

@Preview
@Composable
private fun ListPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Data(sameKeyMapListItems()),
            bottomListPadding = 100.dp,
        )
    }
}

@Preview
@Composable
private fun SelectableListPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Data(sameKeyMapListItems()),
            isSelectable = true,
            bottomListPadding = 100.dp,
        )
    }
}

@Preview
@Composable
private fun EmptyPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Data(emptyList()),
            bottomListPadding = 100.dp,
        )
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Loading,
            bottomListPadding = 100.dp,
        )
    }
}
