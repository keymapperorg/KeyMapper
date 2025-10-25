package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.sds100.keymapper.base.utils.ui.LinkType
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.DraggableItem
import io.github.sds100.keymapper.base.utils.ui.compose.rememberDragDropState
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.camera.CameraLens
import kotlinx.coroutines.flow.update

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
            onEnableProModeClick = viewModel::onEnableProModeForKeyEventActionsClick,
            onEnableInputMethodClick = viewModel::onEnableImeClick,
            onChooseInputMethodClick = viewModel::onChooseImeClick,
            onDoneClick = viewModel::dismissFixKeyEventActionBottomSheet,
            onSelectProMode = viewModel::onSelectProMode,
            onSelectInputMethod = viewModel::onSelectInputMethod,
            onAutoSwitchImeCheckedChange = viewModel::onAutoSwitchImeCheckedChange,
        )
    }

    HandleActionBottomSheets(viewModel.createActionDelegate)

    ActionsScreen(
        modifier = modifier,
        state = state,
        tipModel = actionTipModel,
        onRemoveClick = viewModel::onRemoveClick,
        onEditClick = viewModel::onEditClick,
        onMoveAction = viewModel::onMoveAction,
        onFixErrorClick = viewModel::onFixError,
        onClickShortcut = viewModel::onClickShortcut,
        onTestClick = viewModel::onTestClick,
        onAddClick = viewModel::onAddActionClick,
        onActionTipDismiss = viewModel::onActionTipDismissClick,
        onTipButtonClick = viewModel::onTipButtonClick,
    )
}

@Composable
private fun ActionsScreen(
    modifier: Modifier = Modifier,
    state: State<ConfigActionsState>,
    tipModel: OnboardingTipModel? = null,
    onAddClick: () -> Unit = {},
    onRemoveClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onMoveAction: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onFixErrorClick: (String) -> Unit = {},
    onTestClick: (String) -> Unit = {},
    onClickShortcut: (ActionData) -> Unit = {},
    onActionTipDismiss: () -> Unit = {},
    onTipButtonClick: (String) -> Unit = {},
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var actionToDelete by rememberSaveable { mutableStateOf<String?>(null) }

    if (showDeleteDialog && actionToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(stringResource(R.string.action_list_delete_dialog_title))
            },
            text = { Text(stringResource(R.string.action_list_delete_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveClick(actionToDelete!!)
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.action_list_delete_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_list_delete_cancel))
                }
            },
        )
    }

    when (state) {
        State.Loading -> Loading()
        is State.Data<ConfigActionsState> -> Surface(modifier = modifier) {
            Column {
                Spacer(Modifier.height(8.dp))

                // Display action tip if available
                tipModel?.let { tip ->
                    TipCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        title = tip.title,
                        message = tip.message,
                        isDismissable = tip.isDismissable,
                        onDismiss = onActionTipDismiss,
                        buttonText = tip.buttonText,
                        onButtonClick = { onTipButtonClick(tip.id) },
                    )

                    Spacer(Modifier.height(8.dp))
                }

                when (val data = state.data) {
                    is ConfigActionsState.Empty -> {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth(),
                                text = stringResource(R.string.actions_recyclerview_placeholder),
                                textAlign = TextAlign.Center,
                            )

                            if (data.shortcuts.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.recently_used_actions),
                                    style = MaterialTheme.typography.titleSmall,
                                )

                                Spacer(Modifier.height(8.dp))

                                ShortcutRow(
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp)
                                        .fillMaxWidth(),
                                    shortcuts = data.shortcuts,
                                    onClick = onClickShortcut,
                                )
                            }
                        }
                    }

                    is ConfigActionsState.Loaded -> {
                        if (data.actions.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = stringResource(R.string.action_list_explanation_header),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        ActionList(
                            modifier = Modifier.weight(1f),
                            actionList = data.actions,
                            shortcuts = data.shortcuts,
                            isReorderingEnabled = data.isReorderingEnabled,
                            onRemoveClick = {
                                actionToDelete = it
                                showDeleteDialog = true
                            },
                            onEditClick = onEditClick,
                            onFixErrorClick = onFixErrorClick,
                            onMove = onMoveAction,
                            onClickShortcut = onClickShortcut,
                            onTestClick = onTestClick,
                        )
                    }
                }

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

@Composable
private fun ActionList(
    modifier: Modifier = Modifier,
    actionList: List<ActionListItemModel>,
    shortcuts: Set<ShortcutModel<ActionData>>,
    isReorderingEnabled: Boolean,
    onRemoveClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onFixErrorClick: (String) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onClickShortcut: (ActionData) -> Unit,
    onTestClick: (String) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        onMove = onMove,
        // Do not drag and drop the row of shortcuts
        ignoreLastItems = if (shortcuts.isEmpty()) {
            0
        } else {
            1
        },
    )

    // Use dragContainer rather than .draggable() modifier because that causes
    // dragging the first item to be always be dropped in the next position.
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        itemsIndexed(
            actionList,
            key = { _, item -> item.id },
            contentType = { _, _ -> "action" },
        ) { index, model ->
            DraggableItem(
                dragDropState = dragDropState,
                index = index,
            ) { isDragging ->
                ActionListItem(
                    modifier = Modifier.fillMaxWidth(),
                    model = model,
                    index = index,
                    isDraggingEnabled = actionList.size > 1,
                    isDragging = isDragging,
                    isReorderingEnabled = isReorderingEnabled,
                    dragDropState = dragDropState,
                    onEditClick = { onEditClick(model.id) },
                    onRemoveClick = { onRemoveClick(model.id) },
                    onFixClick = { onFixErrorClick(model.id) },
                    onTestClick = { onTestClick(model.id) },
                )
            }
        }

        if (shortcuts.isNotEmpty()) {
            item(key = "shortcuts", contentType = "shortcuts") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.recently_used_actions),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    ShortcutRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        shortcuts = shortcuts,
                        onClick = { onClickShortcut(it) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun EmptyPreview() {
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
                            text = "Pinch in with 2 finger(s) on coordinates 5/4 with a pinch distance of 8px in 200ms",
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
            state = State.Data(
                ConfigActionsState.Loaded(
                    actions = listOf(
                        ActionListItemModel(
                            id = "1",
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Toggle Back flashlight",
                            secondaryText = "Repeat until released",
                            error = "Flashlight not found",
                            isErrorFixable = true,
                            linkType = LinkType.ARROW,
                        ),
                        ActionListItemModel(
                            id = "2",
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Toggle Back flashlight",
                            secondaryText = "Repeat until released",
                            error = null,
                            isErrorFixable = true,
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
                            text = "Pinch in with 2 finger(s) on coordinates 5/4 with a pinch distance of 8px in 200ms",
                            data = ActionData.ConsumeKeyEvent,
                        ),
                    ),
                    isReorderingEnabled = true,
                ),
            ),
        )
    }
}
