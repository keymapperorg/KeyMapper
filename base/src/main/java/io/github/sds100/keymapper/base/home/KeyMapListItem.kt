package io.github.sds100.keymapper.base.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.utils.ui.compose.CompactChip
import io.github.sds100.keymapper.base.utils.ui.compose.CompactErrorButton
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.ErrorCompactChip

@Composable
fun KeyMapListItem(
    modifier: Modifier = Modifier,
    isSelectable: Boolean,
    model: KeyMapListItemModel,
    onClickKeyMap: () -> Unit,
    onLongClickKeyMap: () -> Unit,
    onSelectedChange: (Boolean) -> Unit,
    onFixClick: () -> Unit,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TriggerDescription(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                        triggerKeys = model.content.triggerKeys,
                        separator = model.content.triggerSeparatorIcon,
                    )

                    if (model.content.isEnabled && model.content.hasError) {
                        CompactErrorButton(onClick = onFixClick) {
                            Text(stringResource(R.string.button_fix))
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

val chipHeight = 28.dp

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
private fun ActionConstraintChip(model: ComposeChipModel, onFixClick: () -> Unit) {
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
            onClick = onFixClick,
            text = model.text,
            enabled = model.isFixable,
        )
    }
}

@PreviewLightDark
@Composable
private fun FullContentPreview() {
    KeyMapperTheme {
        KeyMapListItem(
            isSelectable = false,
            model = sameKeyMapListItems()[0],
            onClickKeyMap = {},
            onLongClickKeyMap = {},
            onSelectedChange = {},
            onFixClick = {},
        )
    }
}

@Preview
@Composable
private fun MultilineOptionsPreview() {
    KeyMapperTheme {
        KeyMapListItem(
            isSelectable = false,
            model = sameKeyMapListItems()[1],
            onClickKeyMap = {},
            onLongClickKeyMap = {},
            onSelectedChange = {},
            onFixClick = {},
        )
    }
}

@Preview
@Composable
private fun DisabledPreview() {
    KeyMapperTheme {
        KeyMapListItem(
            isSelectable = false,
            model = sameKeyMapListItems()[2],
            onClickKeyMap = {},
            onLongClickKeyMap = {},
            onSelectedChange = {},
            onFixClick = {},
        )
    }
}

@Preview
@Composable
private fun MinimalPreview() {
    KeyMapperTheme {
        KeyMapListItem(
            isSelectable = false,
            model = sameKeyMapListItems()[3],
            onClickKeyMap = {},
            onLongClickKeyMap = {},
            onSelectedChange = {},
            onFixClick = {},
        )
    }
}

@Preview
@Composable
private fun EmptyTriggerPreview() {
    KeyMapperTheme {
        KeyMapListItem(
            isSelectable = false,
            model = sameKeyMapListItems()[4],
            onClickKeyMap = {},
            onLongClickKeyMap = {},
            onSelectedChange = {},
            onFixClick = {},
        )
    }
}

@Preview
@Composable
private fun SelectableCheckedPreview() {
    KeyMapperTheme {
        KeyMapListItem(
            isSelectable = true,
            model = sameKeyMapListItems()[0],
            onClickKeyMap = {},
            onLongClickKeyMap = {},
            onSelectedChange = {},
            onFixClick = {},
        )
    }
}

@Preview
@Composable
private fun SelectableUncheckedPreview() {
    KeyMapperTheme {
        KeyMapListItem(
            isSelectable = true,
            model = sameKeyMapListItems()[4],
            onClickKeyMap = {},
            onLongClickKeyMap = {},
            onSelectedChange = {},
            onFixClick = {},
        )
    }
}
