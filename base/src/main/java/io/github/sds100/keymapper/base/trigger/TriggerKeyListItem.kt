package io.github.sds100.keymapper.base.trigger

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assistant
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.FingerprintGestureType
import io.github.sds100.keymapper.base.util.ui.LinkType
import io.github.sds100.keymapper.base.util.ui.compose.DragDropState

@Composable
fun TriggerKeyListItem(
    modifier: Modifier = Modifier,
    model: TriggerKeyListItemModel,
    index: Int,
    isDraggingEnabled: Boolean = false,
    isDragging: Boolean,
    isReorderingEnabled: Boolean,
    dragDropState: DragDropState? = null,
    onEditClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onFixClick: (TriggerError) -> Unit = {},
) {
    val draggableState = rememberDraggableState {
        dragDropState?.onDrag(Offset(0f, it))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .height(IntrinsicSize.Min)
                .padding(start = 16.dp, end = 16.dp)
                .draggable(
                    state = draggableState,
                    enabled = isDraggingEnabled,
                    orientation = Orientation.Vertical,
                    startDragImmediately = false,
                    onDragStarted = { offset ->
                        dragDropState?.onDragStart(index, offset)
                    },
                    onDragStopped = { dragDropState?.onDragInterrupted() },
                ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isDragging) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(8.dp))

                if (isReorderingEnabled) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.width(8.dp))

                // To save space only show the icon if there is no error.
                if (model.error == null) {
                    val icon = when (model) {
                        is TriggerKeyListItemModel.Assistant -> Icons.Outlined.Assistant
                        is TriggerKeyListItemModel.FloatingButton -> Icons.Outlined.BubbleChart
                        is TriggerKeyListItemModel.FingerprintGesture -> Icons.Outlined.Fingerprint
                        else -> null
                    }

                    if (icon != null) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                val primaryText = when (model) {
                    is TriggerKeyListItemModel.Assistant -> when (model.assistantType) {
                        AssistantTriggerType.ANY -> stringResource(R.string.assistant_any_trigger_name)
                        AssistantTriggerType.VOICE -> stringResource(R.string.assistant_voice_trigger_name)
                        AssistantTriggerType.DEVICE -> stringResource(R.string.assistant_device_trigger_name)
                    }

                    is TriggerKeyListItemModel.FloatingButton -> stringResource(
                        R.string.trigger_key_floating_button_description,
                        model.buttonName,
                    )

                    is TriggerKeyListItemModel.KeyCode -> model.keyName

                    is TriggerKeyListItemModel.FloatingButtonDeleted -> stringResource(R.string.trigger_error_floating_button_deleted_title)

                    is TriggerKeyListItemModel.FingerprintGesture -> when (model.gestureType) {
                        FingerprintGestureType.SWIPE_UP -> stringResource(R.string.trigger_key_fingerprint_gesture_up)
                        FingerprintGestureType.SWIPE_DOWN -> stringResource(R.string.trigger_key_fingerprint_gesture_down)
                        FingerprintGestureType.SWIPE_LEFT -> stringResource(R.string.trigger_key_fingerprint_gesture_left)
                        FingerprintGestureType.SWIPE_RIGHT -> stringResource(R.string.trigger_key_fingerprint_gesture_right)
                    }
                }

                Spacer(Modifier.width(8.dp))

                if (model.error == null) {
                    val clickTypeString = when (model.clickType) {
                        ClickType.SHORT_PRESS -> null
                        ClickType.LONG_PRESS -> stringResource(R.string.clicktype_long_press)
                        ClickType.DOUBLE_PRESS -> stringResource(R.string.clicktype_double_press)
                    }

                    val tertiaryText = when (model) {
                        is TriggerKeyListItemModel.KeyCode -> model.extraInfo
                        is TriggerKeyListItemModel.FloatingButton -> model.layoutName

                        else -> null
                    }

                    TextColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        primaryText = primaryText,
                        secondaryText = clickTypeString,
                        tertiaryText = tertiaryText,
                    )
                } else {
                    ErrorTextColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        primaryText = primaryText,
                        errorText = getErrorMessage(model.error!!),
                    )
                }

                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides 16.dp,
                ) {
                    if (model.error != null && model.error?.isFixable ?: false) {
                        FilledTonalButton(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                            onClick = { onFixClick(model.error!!) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.button_fix),
                            )
                        }
                    }

                    if (model !is TriggerKeyListItemModel.FloatingButtonDeleted) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.trigger_key_list_item_edit),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.trigger_key_list_item_remove),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        if (model.linkType == LinkType.HIDDEN) {
            // Important! Show an empty spacer so the height of the card remains constant
            // while dragging. If the height changes while dragging it can lead to janky
            // behavior.
            Spacer(Modifier.height(32.dp))
        } else {
            Spacer(Modifier.height(4.dp))

            Icon(
                imageVector = when (model.linkType) {
                    LinkType.ARROW -> Icons.Rounded.ArrowDownward
                    LinkType.PLUS -> Icons.Rounded.Add
                    LinkType.HIDDEN -> Icons.Rounded.Add
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun getErrorMessage(error: TriggerError): String {
    return when (error) {
        TriggerError.DND_ACCESS_DENIED -> stringResource(R.string.trigger_error_dnd_access_denied)
        TriggerError.SCREEN_OFF_ROOT_DENIED -> stringResource(R.string.trigger_error_screen_off_root_permission_denied)
        TriggerError.CANT_DETECT_IN_PHONE_CALL -> stringResource(R.string.trigger_error_cant_detect_in_phone_call)
        TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> stringResource(R.string.trigger_error_assistant_not_purchased)
        TriggerError.DPAD_IME_NOT_SELECTED -> stringResource(R.string.trigger_error_dpad_ime_not_selected)
        TriggerError.FLOATING_BUTTON_DELETED -> stringResource(R.string.trigger_error_floating_button_deleted)
        TriggerError.FLOATING_BUTTONS_NOT_PURCHASED -> stringResource(R.string.trigger_error_floating_buttons_not_purchased)
        TriggerError.PURCHASE_VERIFICATION_FAILED -> stringResource(R.string.trigger_error_product_verification_failed)
    }
}

@Composable
private fun TextColumn(
    modifier: Modifier = Modifier,
    primaryText: String,
    secondaryText: String? = null,
    tertiaryText: String? = null,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = primaryText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (secondaryText != null) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (tertiaryText != null) {
            Text(
                text = tertiaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ErrorTextColumn(
    modifier: Modifier = Modifier,
    primaryText: String,
    errorText: String,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = primaryText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = errorText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview
@Composable
private fun KeyCodePreview() {
    TriggerKeyListItem(
        model = TriggerKeyListItemModel.KeyCode(
            id = "id",
            keyName = "Volume Up",
            clickType = ClickType.SHORT_PRESS,
            extraInfo = "External Keyboard",
            linkType = LinkType.ARROW,
            error = null,
        ),
        isDragging = false,
        isReorderingEnabled = true,
        index = 0,
    )
}

@Preview
@Composable
private fun NoDragPreview() {
    TriggerKeyListItem(
        model = TriggerKeyListItemModel.KeyCode(
            id = "id",
            keyName = "Volume Up",
            clickType = ClickType.LONG_PRESS,
            extraInfo = "External Keyboard",
            linkType = LinkType.ARROW,
            error = null,
        ),
        isDragging = false,
        isReorderingEnabled = false,
        index = 0,
    )
}

@Preview
@Composable
private fun AssistantPreview() {
    TriggerKeyListItem(
        model = TriggerKeyListItemModel.Assistant(
            id = "id",
            assistantType = AssistantTriggerType.DEVICE,
            clickType = ClickType.SHORT_PRESS,
            linkType = LinkType.ARROW,
            error = null,
        ),
        isDragging = false,
        isReorderingEnabled = true,
        index = 0,
    )
}

@Preview
@Composable
private fun AssistantErrorPreview() {
    TriggerKeyListItem(
        model = TriggerKeyListItemModel.Assistant(
            id = "id",
            assistantType = AssistantTriggerType.DEVICE,
            clickType = ClickType.DOUBLE_PRESS,
            linkType = LinkType.ARROW,
            error = TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED,
        ),
        isDragging = false,
        isReorderingEnabled = true,
        index = 0,
    )
}

@Preview
@Composable
private fun FloatingButtonPreview() {
    TriggerKeyListItem(
        model = TriggerKeyListItemModel.FloatingButton(
            id = "id",
            buttonName = "😎",
            layoutName = "Gaming",
            clickType = ClickType.DOUBLE_PRESS,
            linkType = LinkType.ARROW,
            error = null,
        ),
        isDragging = false,
        isReorderingEnabled = false,
        index = 0,
    )
}

@Preview
@Composable
private fun FloatingButtonErrorPreview() {
    TriggerKeyListItem(
        model = TriggerKeyListItemModel.FloatingButtonDeleted(
            id = "id",
            clickType = ClickType.DOUBLE_PRESS,
            linkType = LinkType.ARROW,
        ),
        isDragging = false,
        isReorderingEnabled = false,
        index = 0,
    )
}
