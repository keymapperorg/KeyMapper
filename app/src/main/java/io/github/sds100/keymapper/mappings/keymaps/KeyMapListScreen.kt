package io.github.sds100.keymapper.mappings.keymaps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo

@Composable
fun KeyMapListScreen(
    modifier: Modifier = Modifier,
    listItems: State<List<KeyMapListItemModel>>,
    onClickKeyMap: (String) -> Unit = {},
    onLongClickKeyMap: (String) -> Unit = {},
) {
    Surface(modifier = modifier) {
        when (listItems) {
            is State.Loading -> {}
            is State.Data -> LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listItems.data, key = { it.uid }) { model ->
                    KeyMapListItem(
                        modifier = Modifier.fillMaxWidth(),
                        // TODO select key maps
                        isSelectable = false,
                        model = model,
                        onClickKeyMap = { onClickKeyMap(model.uid) },
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
                    )
                }
            }
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
        Row(modifier = Modifier.padding(start = 16.dp)) {
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
                    .padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                if (model.extraInfo != null) {
                    Text(
                        text = model.extraInfo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                    )
                }

                if (model.triggerDescription != null) {
                    Row {
                        Text(
                            text = stringResource(R.string.trigger_header),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = model.triggerDescription,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (model.triggerErrors.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (error in model.triggerErrors) {
                            ErrorChip(
                                onClick = { onTriggerErrorClick(error) },
                                text = getTriggerErrorMessage(error),
                            )
                        }
                    }
                }

                if (model.actions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.action_list_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (chipModel in model.actions) {
                            ActionConstraintChip(
                                chipModel,
                                onClick = { onFixActionClick(chipModel.id) },
                            )
                        }
                    }
                }

                if (model.constraints.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.constraint_list_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (chipModel in model.constraints) {
                            ActionConstraintChip(
                                chipModel,
                                onClick = { onFixConstraintClick(chipModel.id) },
                            )
                        }
                    }
                }

                if (model.optionsDescription != null) {
                    Text(
                        text = stringResource(R.string.option_list_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = model.optionsDescription,
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
                )
            if (model.icon == null) {
                AssistChip(
                    onClick = {},
                    label = { Text(model.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = colors,
                    border = null,
                )
            } else {
                AssistChip(
                    onClick = {},
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
            uid = "0",
            isSelected = false,
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
        KeyMapListItemModel(
            uid = "1",
            isSelected = false,
            triggerDescription = null,
            actions = emptyList(),
            constraints = emptyList(),
            optionsDescription = null,
            triggerErrors = emptyList(),
            extraInfo = "Disabled • No trigger",
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
