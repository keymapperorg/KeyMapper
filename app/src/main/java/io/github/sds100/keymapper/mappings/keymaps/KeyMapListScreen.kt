package io.github.sds100.keymapper.mappings.keymaps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo

@Composable
fun KeyMapListScreen(modifier: Modifier = Modifier, viewModel: KeyMapListViewModel) {
    val listItems by viewModel.state.collectAsStateWithLifecycle()

    KeyMapListScreen(
        modifier = modifier,
        listItems = listItems,
        // TODO selection
        isSelectable = false,
        onClickKeyMap = viewModel::onKeyMapCardClick,
        onLongClickKeyMap = viewModel::onKeyMapCardLongClick,
        onFixClick = viewModel::onFixClick,
        onTriggerErrorClick = viewModel::onFixTriggerError,
    )
}

@Composable
private fun KeyMapListScreen(
    modifier: Modifier = Modifier,
    listItems: State<List<KeyMapListItemModel>>,
    isSelectable: Boolean = false,
    onClickKeyMap: (String) -> Unit = {},
    onLongClickKeyMap: (String) -> Unit = {},
    onFixClick: (Error) -> Unit = {},
    onTriggerErrorClick: (TriggerError) -> Unit = {},
) {
    Surface(modifier = modifier) {
        when (listItems) {
            is State.Loading -> {
                Box {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            is State.Data -> {
                if (listItems.data.isEmpty()) {
                    EmptyKeyMapList(modifier = modifier)
                } else {
                    KeyMapList(
                        modifier,
                        listItems.data,
                        isSelectable,
                        onClickKeyMap,
                        onFixClick,
                        onTriggerErrorClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyKeyMapList(modifier: Modifier = Modifier) {
    Box(modifier) {
        val shrug = stringResource(R.string.shrug)
        val text = stringResource(R.string.home_key_map_list_empty)
        Text(
            modifier = Modifier.align(Alignment.Center),
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
        )
    }
}

@Composable
private fun KeyMapList(
    modifier: Modifier = Modifier,
    listItems: List<KeyMapListItemModel>,
    isSelectable: Boolean,
    onClickKeyMap: (String) -> Unit,
    onFixClick: (Error) -> Unit,
    onTriggerErrorClick: (TriggerError) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(listItems, key = { it.uid }) { model ->
            KeyMapListItem(
                modifier = Modifier.fillMaxWidth(),
                // TODO select key maps
                isSelectable = isSelectable,
                model = model,
                onClickKeyMap = { onClickKeyMap(model.content.uid) },
                onSelectedChange = { TODO() },
                onFixClick = onFixClick,
                onTriggerErrorClick = onTriggerErrorClick,
            )
        }

        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.home_key_map_list_footer_text),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Give some space at the end of the list so that the FAB doesn't block the items.
        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun KeyMapListItem(
    modifier: Modifier = Modifier,
    isSelectable: Boolean,
    model: KeyMapListItemModel,
    onClickKeyMap: () -> Unit,
    onSelectedChange: (Boolean) -> Unit,
    onFixClick: (Error) -> Unit,
    onTriggerErrorClick: (TriggerError) -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClickKeyMap,
    ) {
        Row(modifier = Modifier.padding(start = 8.dp)) {
            if (isSelectable) {
                Checkbox(
                    checked = model.isSelected,
                    onCheckedChange = onSelectedChange,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                if (model.content.extraInfo != null) {
                    Text(
                        text = model.content.extraInfo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                    )
                }

                if (model.content.triggerKeys.isNotEmpty()) {
                    TriggerDescription(
                        modifier = Modifier.fillMaxWidth(),
                        triggerKeys = model.content.triggerKeys,
                        separator = model.content.triggerSeparatorIcon,
                    )

                    Spacer(Modifier.height(4.dp))
                }

                if (model.content.triggerErrors.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (error in model.content.triggerErrors) {
                            ErrorChip(
                                onClick = { onTriggerErrorClick(error) },
                                text = getTriggerErrorMessage(error),
                            )
                        }
                    }
                }

                if (model.content.actions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.action_list_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (chipModel in model.content.actions) {
                            ActionConstraintChip(
                                chipModel,
                                onFixClick = onFixClick,
                            )
                        }
                    }
                }

                if (model.content.constraints.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.constraint_list_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for ((index, chipModel) in model.content.constraints.withIndex()) {
                            ActionConstraintChip(
                                chipModel,
                                onFixClick = onFixClick,
                            )

                            if (index < model.content.constraints.lastIndex) {
                                when (model.content.constraintMode) {
                                    ConstraintMode.AND -> Text(
                                        stringResource(R.string.constraint_mode_and),
                                        style = MaterialTheme.typography.labelMedium,
                                    )

                                    ConstraintMode.OR -> Text(
                                        stringResource(R.string.constraint_mode_or),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                if (model.content.optionsDescription != null) {
                    Text(
                        text = stringResource(R.string.option_list_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = model.content.optionsDescription,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
        append(stringResource(R.string.trigger_header))
        append(" ")

        for ((index, key) in triggerKeys.withIndex()) {
            append(key)

            if (index < triggerKeys.lastIndex) {
                append(" ")
                appendInlineContent("separator")
                append(" ")
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
private fun ActionConstraintChip(
    model: ComposeChipModel,
    onFixClick: (Error) -> Unit,
) {
    when (model) {
        is ComposeChipModel.Normal -> {
            CompactChip(
                text = model.text,
                icon = model.icon?.let { icon ->
                    {
                        when (icon) {
                            is ComposeIconInfo.Drawable -> Icon(
                                modifier = Modifier.size(18.dp),
                                painter = rememberDrawablePainter(icon.drawable),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )

                            is ComposeIconInfo.Vector -> Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = icon.imageVector,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        }

        is ComposeChipModel.Error -> ErrorChip(onClick = { onFixClick(model.error) }, model.text)
    }
}

@Composable
private fun ErrorChip(
    onClick: () -> Unit,
    text: String,
) {
    CompactChip(
        text = text,
        icon = {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
            )
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        onClick = onClick,
    )
}

@Composable
private fun CompactChip(
    modifier: Modifier = Modifier,
    text: String,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = AssistChipDefaults.shape,
    ) {
        CompactChipContent(icon, text, contentColor)
    }
}

@Composable
private fun CompactChip(
    modifier: Modifier = Modifier,
    text: String,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        Surface(
            modifier = modifier,
            color = containerColor,
            shape = AssistChipDefaults.shape,
            onClick = onClick,
        ) {
            CompactChipContent(icon, text, contentColor)
        }
    }
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

@Composable
private fun getTriggerErrorMessage(error: TriggerError): String {
    return when (error) {
        TriggerError.DND_ACCESS_DENIED -> stringResource(R.string.trigger_error_dnd_access_denied)
        TriggerError.SCREEN_OFF_ROOT_DENIED -> stringResource(R.string.trigger_error_screen_off_root_permission_denied)
        TriggerError.CANT_DETECT_IN_PHONE_CALL -> stringResource(R.string.trigger_error_cant_detect_in_phone_call)
        TriggerError.ASSISTANT_NOT_SELECTED -> stringResource(R.string.trigger_error_assistant_activity_not_chosen)
        TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> stringResource(R.string.trigger_error_assistant_not_purchased)
        TriggerError.DPAD_IME_NOT_SELECTED -> stringResource(R.string.trigger_error_dpad_ime_not_selected)
        TriggerError.FLOATING_BUTTON_DELETED -> stringResource(R.string.trigger_error_floating_button_deleted)
        TriggerError.FLOATING_BUTTONS_NOT_PURCHASED -> stringResource(R.string.trigger_error_floating_buttons_not_purchased)
    }
}

@Composable
private fun sampleList(): List<KeyMapListItemModel> {
    val context = LocalContext.current

    return listOf(
        KeyMapListItemModel(
            isSelected = true,
            KeyMapListItemModel.Content(
                uid = "0",
                triggerKeys = listOf("Volume down", "Volume up"),
                triggerSeparatorIcon = Icons.Outlined.Add,
                actions = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Open Key Mapper",
                    ),
                    ComposeChipModel.Error(
                        id = "1",
                        text = "Input KEYCODE_0 • Repeat until released",
                        error = Error.NoCompatibleImeChosen,
                    ),
                    ComposeChipModel.Normal(
                        id = "2",
                        text = "Input KEYCODE_Q",
                        icon = null,
                    ),
                    ComposeChipModel.Normal(
                        id = "3",
                        text = "Toggle flashlight",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.FlashlightOn),
                    ),
                ),
                constraintMode = ConstraintMode.AND,
                constraints = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Key Mapper is not open",
                    ),
                    ComposeChipModel.Error(
                        id = "1",
                        "Key Mapper is playing media",
                        error = Error.AppNotFound(""),
                    ),
                ),
                optionsDescription = "Vibrate",
                triggerErrors = listOf(TriggerError.DND_ACCESS_DENIED),
                extraInfo = null,
            ),
        ),
        KeyMapListItemModel(
            isSelected = false,
            content = KeyMapListItemModel.Content(
                uid = "1",
                triggerKeys = emptyList(),
                triggerSeparatorIcon = Icons.Outlined.Add,
                actions = emptyList(),
                constraintMode = ConstraintMode.OR,
                constraints = emptyList(),
                optionsDescription = null,
                triggerErrors = emptyList(),
                extraInfo = "Disabled • No trigger",
            ),
        ),
    )
}

@Preview
@Composable
private fun ListPreview() {
    KeyMapperTheme {
        KeyMapListScreen(modifier = Modifier.fillMaxSize(), listItems = State.Data(sampleList()))
    }
}

@Preview
@Composable
private fun SelectableListPreview() {
    KeyMapperTheme {
        KeyMapListScreen(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Data(sampleList()),
            isSelectable = true,
        )
    }
}

@Preview
@Composable
private fun EmptyPreview() {
    KeyMapperTheme {
        KeyMapListScreen(modifier = Modifier.fillMaxSize(), listItems = State.Data(emptyList()))
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    KeyMapperTheme {
        KeyMapListScreen(modifier = Modifier.fillMaxSize(), listItems = State.Loading)
    }
}
