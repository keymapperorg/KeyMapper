package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.keyevent.FixKeyEventActionBottomSheet
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.keymaps.ShortcutModel
import io.github.sds100.keymapper.base.keymaps.ShortcutRow
import io.github.sds100.keymapper.base.onboarding.OnboardingTipModel
import io.github.sds100.keymapper.base.onboarding.TipCard
import io.github.sds100.keymapper.base.utils.ui.SliderMaximums
import io.github.sds100.keymapper.base.utils.ui.SliderMinimums
import io.github.sds100.keymapper.base.utils.ui.SliderStepSizes
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.CustomDialog
import io.github.sds100.keymapper.base.utils.ui.compose.DraggableItem
import io.github.sds100.keymapper.base.utils.ui.compose.SliderOptionText
import io.github.sds100.keymapper.base.utils.ui.compose.TextFieldDialog
import io.github.sds100.keymapper.base.utils.ui.compose.rememberDragDropState
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.camera.CameraLens
import kotlinx.coroutines.flow.update

/**
 * The height of the row between action cards. The last card has a spacer of the same height so
 * the height of the items stays constant while dragging.
 */
private val linkRowHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen(modifier: Modifier = Modifier, viewModel: ConfigActionsViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val optionsState by viewModel.actionOptionsState.collectAsStateWithLifecycle()
    val actionTipModel by viewModel.actionsTip.collectAsStateWithLifecycle()

    if (optionsState != null) {
        ActionOptionsBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            sheetState = sheetState,
            state = optionsState!!,
            onDismissRequest = { viewModel.actionOptionsUid.update { null } },
            callback = viewModel,
        )
    }

    val fixKeyEventActionState by viewModel.fixKeyEventActionState.collectAsStateWithLifecycle()

    if (fixKeyEventActionState != null) {
        FixKeyEventActionBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            state = fixKeyEventActionState!!,
            sheetState = sheetState,
            onDismissRequest = viewModel::dismissFixKeyEventActionBottomSheet,
            onEnableAccessibilityServiceClick = viewModel::onEnableAccessibilityServiceClick,
            onEnableExpertModeClick = viewModel::onEnableExpertModeForKeyEventActionsClick,
            onEnableInputMethodClick = viewModel::onEnableImeClick,
            onChooseInputMethodClick = viewModel::onChooseImeClick,
            onDoneClick = viewModel::dismissFixKeyEventActionBottomSheet,
            onSelectExpertMode = viewModel::onSelectExpertMode,
            onSelectInputMethod = viewModel::onSelectInputMethod,
            onAutoSwitchImeCheckedChange = viewModel::onAutoSwitchImeCheckedChange,
        )
    }

    HandleActionBottomSheets(viewModel.createActionDelegate)

    ActionsScreen(
        modifier = modifier,
        state = state,
        tipModel = actionTipModel,
        callback = viewModel,
        onRemoveClick = viewModel::onRemoveClick,
        onAddClick = viewModel::onAddActionClick,
        onDelayChange = viewModel::onDelayChanged,
        onRenameAction = viewModel::onRenameAction,
    )
}

@Composable
private fun ActionsScreen(
    modifier: Modifier = Modifier,
    state: State<ConfigActionsState>,
    tipModel: OnboardingTipModel? = null,
    callback: ActionListCallback = object : ActionListCallback {},
    onAddClick: () -> Unit = {},
    onRemoveClick: (String) -> Unit = {},
    onDelayChange: (String, Int) -> Unit = { _, _ -> },
    onRenameAction: (String, String) -> Unit = { _, _ -> },
) {
    var actionToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var actionToRename by rememberSaveable { mutableStateOf<String?>(null) }
    var actionToSetDelay by rememberSaveable { mutableStateOf<String?>(null) }

    val actions = ((state as? State.Data)?.data as? ConfigActionsState.Loaded)?.actions.orEmpty()

    if (actionToDelete != null) {
        AlertDialog(
            onDismissRequest = { actionToDelete = null },
            title = {
                Text(stringResource(R.string.action_list_delete_dialog_title))
            },
            text = { Text(stringResource(R.string.action_list_delete_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveClick(actionToDelete!!)
                        actionToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.action_list_delete_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { actionToDelete = null }) {
                    Text(stringResource(R.string.action_list_delete_cancel))
                }
            },
        )
    }

    val renameModel = actions.find { it.id == actionToRename }

    if (renameModel != null) {
        TextFieldDialog(
            title = stringResource(R.string.action_options_custom_name_dialog_title),
            submitButtonText = stringResource(R.string.pos_save),
            initialText = renameModel.title,
            hint = renameModel.title,
            canBeEmpty = true,
            onSubmitClick = { newText ->
                onRenameAction(renameModel.id, newText)
                null
            },
            onDismissRequest = { actionToRename = null },
        )
    }

    val delayModel = actions.find { it.id == actionToSetDelay }

    if (delayModel != null) {
        DelayBeforeNextActionDialog(
            initialDelay = delayModel.delayBeforeNextAction,
            onSaveClick = { delay ->
                onDelayChange(delayModel.id, delay)
                actionToSetDelay = null
            },
            onDismissRequest = { actionToSetDelay = null },
        )
    }

    when (state) {
        State.Loading -> Loading()

        is State.Data<ConfigActionsState> -> Surface(modifier = modifier) {
            Column {
                ActionList(
                    modifier = Modifier.weight(1f),
                    state = state.data,
                    tipModel = tipModel,
                    callback = callback,
                    onRemoveClick = { actionToDelete = it },
                    onDelayClick = { actionToSetDelay = it },
                    onRenameClick = { actionToRename = it },
                )

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = onAddClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.button_add_action))
                }
            }
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

interface ActionListCallback {
    fun onEditClick(id: String) = run { }
    fun onFixErrorClick(id: String) = run { }
    fun onMove(fromIndex: Int, toIndex: Int) = run { }
    fun onClickShortcut(data: ActionData) = run { }
    fun onTestClick(id: String) = run { }
    fun onDuplicateClick(id: String) = run { }
    fun onActionTipDismiss() = run { }
    fun onTipButtonClick(id: String) = run { }
    fun onEnabledChange(id: String, enabled: Boolean) = run { }
    fun onExpandedChange(id: String, expanded: Boolean) = run {}
}

@Composable
private fun ActionList(
    modifier: Modifier = Modifier,
    state: ConfigActionsState,
    tipModel: OnboardingTipModel?,
    callback: ActionListCallback,
    onRemoveClick: (String) -> Unit,
    onDelayClick: (String) -> Unit,
    onRenameClick: (String) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    val actions = (state as? ConfigActionsState.Loaded)?.actions.orEmpty()
    val actionIds = remember(actions) { actions.map { it.id } }

    // Only the actions can be dragged. Not the tip or the row of shortcuts.
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        keys = actionIds,
        onMove = callback::onMove,
    )

    // Use dragContainer rather than .draggable() modifier because that causes
    // dragging the first item to be always be dropped in the next position.
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = if (state is ConfigActionsState.Empty) {
            Arrangement.Center
        } else {
            Arrangement.Top
        },
    ) {
        // Display action tip if available
        tipModel?.let { tip ->
            item {
                TipCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    title = tip.title,
                    message = tip.message,
                    isDismissable = tip.isDismissable,
                    onDismiss = callback::onActionTipDismiss,
                    buttonText = tip.buttonText,
                    onButtonClick = { callback.onTipButtonClick(tip.id) },
                )

                Spacer(Modifier.height(16.dp))
            }
        }

        when (state) {
            is ConfigActionsState.Empty -> {
                item {
                    Text(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        text = stringResource(R.string.actions_recyclerview_placeholder),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is ConfigActionsState.Loaded -> {
                val orderedActions = dragDropState.ordered(state.actions) { it.id }

                itemsIndexed(
                    orderedActions,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "action" },
                ) { index, model ->

                    DraggableItem(
                        dragDropState = dragDropState,
                        key = model.id,
                    ) { isDragging ->
                        Column {
                            ActionListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                model = model,
                                index = index,
                                isExpanded = model.isExpanded,
                                isDraggingEnabled = orderedActions.size > 1,
                                isDragging = isDragging,
                                isReorderingEnabled = state.isReorderingEnabled,
                                dragDropState = dragDropState,
                                onExpandedChange = { callback.onExpandedChange(model.id, it) },
                                onEditClick = { callback.onEditClick(model.id) },
                                onRemoveClick = { onRemoveClick(model.id) },
                                onFixClick = { callback.onFixErrorClick(model.id) },
                                onTestClick = { callback.onTestClick(model.id) },
                                onDuplicateClick = { callback.onDuplicateClick(model.id) },
                                onRenameClick = { onRenameClick(model.id) },
                                onEnabledChange = { callback.onEnabledChange(model.id, it) },
                                onMoveUp = if (state.isReorderingEnabled && index > 0) {
                                    { callback.onMove(index, index - 1) }
                                } else {
                                    null
                                },
                                onMoveDown = if (state.isReorderingEnabled &&
                                    index < orderedActions.size - 1
                                ) {
                                    { callback.onMove(index, index + 1) }
                                } else {
                                    null
                                },
                            )

                            if (model.showDelayChip) {
                                ActionLinkRow(
                                    isEnabled = model.isEnabled,
                                    delayBeforeNextAction = model.delayBeforeNextAction,
                                    onDelayClick = { onDelayClick(model.id) },
                                )
                            } else if (index != orderedActions.lastIndex) {
                                // Important! Keep the height of the item constant while dragging.
                                // If the height changes while dragging it can lead to janky
                                // behavior.
                                //
                                // But do not show a gap at the bottom of the list.
                                Spacer(Modifier.height(linkRowHeight))
                            }
                        }
                    }
                }
            }
        }

        if (state.shortcuts.isNotEmpty()) {
            item(key = "shortcuts", contentType = "shortcuts") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.recently_used_actions),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    ShortcutRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        shortcuts = state.shortcuts,
                        onClick = { callback.onClickShortcut(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionLinkRow(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    delayBeforeNextAction: Int?,
    onDelayClick: () -> Unit,
) {
    val colors = if (isEnabled) {
        AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            leadingIconContentColor = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            leadingIconContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(linkRowHeight)
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center),
            imageVector = Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )

        AssistChip(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onDelayClick,
            label = {
                if (delayBeforeNextAction == null) {
                    Text(stringResource(R.string.action_list_add_delay))
                } else {
                    if (delayBeforeNextAction < 1000) {
                        Text(stringResource(R.string.action_title_wait_ms, delayBeforeNextAction))
                    } else {
                        val seconds = delayBeforeNextAction / 1000f
                        val secondsText = if (seconds % 1f == 0f) {
                            seconds.toInt().toString()
                        } else {
                            String.format(LocalLocale.current.platformLocale, "%.1f", seconds)
                        }

                        Text(
                            stringResource(
                                R.string.action_title_wait_secs,
                                secondsText,
                            ),
                        )
                    }
                }
            },
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                    imageVector = if (delayBeforeNextAction == null) {
                        Icons.Rounded.MoreTime
                    } else {
                        Icons.Rounded.Timelapse
                    },
                    contentDescription = null,
                )
            },
            colors = colors,
            border = null,
        )
    }
}

@Composable
private fun DelayBeforeNextActionDialog(
    initialDelay: Int?,
    onSaveClick: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var delay by rememberSaveable {
        mutableIntStateOf(initialDelay ?: SliderStepSizes.DELAY_BEFORE_NEXT_ACTION)
    }

    CustomDialog(
        title = stringResource(R.string.action_options_delay_header),
        confirmButton = {
            TextButton(onClick = { onSaveClick(delay) }) {
                Text(stringResource(R.string.pos_save))
            }
        },
        dismissButton = {
            if (initialDelay == null) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.neg_cancel))
                }
            } else {
                TextButton(onClick = { onSaveClick(0) }) {
                    Text(stringResource(R.string.action_list_delay_remove))
                }
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        val delayMin = SliderMinimums.DELAY_BEFORE_NEXT_ACTION.toFloat()
        val delayMax = SliderMaximums.DELAY_BEFORE_NEXT_ACTION.toFloat()

        SliderOptionText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = null,
            defaultValue = delayMin,
            value = delay.toFloat(),
            valueText = { "${it.toInt()} ms" },
            onValueChange = { delay = it.toInt() },
            valueRange = delayMin..delayMax,
            stepSize = SliderStepSizes.DELAY_BEFORE_NEXT_ACTION,
        )
    }
}

@Preview
@Composable
private fun EmptyPreview() {
    KeyMapperTheme {
        ActionsScreen(
            state = State.Data(
                ConfigActionsState.Empty(shortcuts = emptySet()),
            ),
        )
    }
}

@Preview
@Composable
private fun EmptyWithShortcutsPreview() {
    KeyMapperTheme {
        ActionsScreen(
            state = State.Data(
                ConfigActionsState.Empty(
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Toggle Back flashlight",
                            data = ActionData.Flashlight.Toggle(
                                lens = CameraLens.BACK,
                                strengthPercent = null,
                            ),
                        ),
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.Pinch),
                            text = "Pinch in with 2 finger(s) on coordinates 5/4",
                            data = ActionData.ConsumeKeyEvent,
                        ),
                    ),
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun LoadedPreview() {
    KeyMapperTheme {
        ActionsScreen(
            tipModel = OnboardingTipModel(
                id = "id",
                title = "Use a dedicated action instead",
                message = "Use the \"Flashlight\" action instead.",
                isDismissable = false,
                buttonText = "Replace action",
            ),
            state = State.Data(
                ConfigActionsState.Loaded(
                    actions = listOf(
                        ActionListItemModel(
                            id = "1",
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            title = "Toggle Back flashlight",
                            summary = "Repeat 5x until pressed again • Hold down",
                            error = "Flashlight not found",
                            isErrorFixable = true,
                            showDelayChip = true,
                        ),
                        ActionListItemModel(
                            id = "2",
                            icon = ComposeIconInfo.Vector(Icons.Rounded.Pinch),
                            title = "Open magnifier",
                            isCustomName = true,
                            showDelayChip = true,
                            delayBeforeNextAction = 100,
                            isEnabled = false,
                        ),
                        ActionListItemModel(
                            id = "3",
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            title = "Toggle Back flashlight",
                        ),
                    ),
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Toggle Back flashlight",
                            data = ActionData.Flashlight.Toggle(
                                lens = CameraLens.BACK,
                                strengthPercent = null,
                            ),
                        ),
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.Pinch),
                            text = "Pinch in with 2 finger(s) on coordinates 5/4",
                            data = ActionData.ConsumeKeyEvent,
                        ),
                    ),
                    isReorderingEnabled = true,
                ),
            ),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DelayDialogPreview() {
    KeyMapperTheme {
        DelayBeforeNextActionDialog(
            initialDelay = 400,
            onSaveClick = {},
            onDismissRequest = {},
        )
    }
}
