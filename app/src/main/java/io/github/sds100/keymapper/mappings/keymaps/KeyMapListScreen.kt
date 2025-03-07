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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    )
}

@Composable
private fun KeyMapListScreen(
    modifier: Modifier = Modifier,
    listItems: State<List<KeyMapListItemModel>>,
    isSelectable: Boolean = false,
    onClickKeyMap: (String) -> Unit = {},
    onLongClickKeyMap: (String) -> Unit = {},
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
                    KeyMapList(modifier, listItems.data, isSelectable, onClickKeyMap)
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
                onFixActionClick = { TODO() },
                onFixConstraintClick = { TODO() },
                onTriggerErrorClick = { TODO() },
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
fun KeyMapListItem(
    modifier: Modifier = Modifier,
    isSelectable: Boolean,
    model: KeyMapListItemModel,
    onClickKeyMap: () -> Unit,
    onSelectedChange: (Boolean) -> Unit,
    onFixActionClick: (String) -> Unit,
    onFixConstraintClick: (String) -> Unit,
    onTriggerErrorClick: (TriggerError) -> Unit,
) {
    OutlinedCard(modifier = modifier, onClick = onClickKeyMap) {
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

                if (model.content.triggerDescription != null) {
                    Row {
                        Text(
                            text = stringResource(R.string.trigger_header),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = model.content.triggerDescription,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (chipModel in model.content.actions) {
                            ActionConstraintChip(
                                chipModel,
                                onClick = { onFixActionClick(chipModel.id) },
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        for ((index, chipModel) in model.content.constraints.withIndex()) {
                            ActionConstraintChip(
                                chipModel,
                                onClick = { onFixConstraintClick(chipModel.id) },
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
private fun ActionConstraintChip(model: ComposeChipModel, onClick: () -> Unit) {
    when (model) {
        is ComposeChipModel.Normal -> {
            val colors =
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    leadingIconContentColor = Color.Unspecified,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledLeadingIconContentColor = Color.Unspecified,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                )
            if (model.icon == null) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(model.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = colors,
                    border = null,
                )
            } else {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(model.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        when (model.icon) {
                            is ComposeIconInfo.Drawable -> Icon(
                                modifier = Modifier.size(18.dp),
                                painter = rememberDrawablePainter(model.icon.drawable),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )

                            is ComposeIconInfo.Vector -> Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = model.icon.imageVector,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = colors,
                    border = null,
                )
            }
        }

        is ComposeChipModel.Error -> ErrorChip(onClick, model.text)
    }
}

@Composable
private fun ErrorChip(
    onClick: () -> Unit,
    text: String,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        border = null,
    )
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
                triggerDescription = "Volume down + Volume up",
                actions = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Open Key Mapper",
                    ),
                    ComposeChipModel.Error(
                        id = "1",
                        text = "Input KEYCODE_0 • Repeat until released",
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
                triggerDescription = null,
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
