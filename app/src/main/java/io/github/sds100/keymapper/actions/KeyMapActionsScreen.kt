package io.github.sds100.keymapper.actions

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.draggable.DraggableItem
import io.github.sds100.keymapper.compose.draggable.rememberDragDropState
import io.github.sds100.keymapper.mappings.ShortcutModel
import io.github.sds100.keymapper.mappings.ShortcutRow
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.LinkType
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyMapActionsScreen(modifier: Modifier = Modifier, viewModel: ConfigActionsViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val optionsState by viewModel.actionOptionsState.collectAsStateWithLifecycle()

    if (optionsState != null) {
        ActionOptionsBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            sheetState = sheetState,
            state = optionsState!!,
            onDismissRequest = { viewModel.actionOptionsUid.update { null } },
            callback = viewModel,
        )
    }

    KeyMapActionsScreen(
        modifier = modifier,
        state = state,
        onRemoveClick = viewModel::onRemoveClick,
        onEditClick = viewModel::onEditClick,
        onMoveAction = viewModel::onMoveAction,
        onFixErrorClick = viewModel::onFixError,
        onClickShortcut = viewModel::onClickShortcut,
        onTestClick = viewModel::onTestClick,
        onAddClick = viewModel::onAddActionClick,
    )
}

@Composable
private fun KeyMapActionsScreen(
    modifier: Modifier = Modifier,
    state: State<ConfigActionsState>,
    onAddClick: () -> Unit = {},
    onRemoveClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onMoveAction: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onFixErrorClick: (String) -> Unit = {},
    onTestClick: (String) -> Unit = {},
    onClickShortcut: (ActionData) -> Unit = {},
) {
    when (state) {
        State.Loading -> Loading()
        is State.Data<ConfigActionsState> -> Surface(modifier = modifier) {
            Column {
                when (state.data) {
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

                            if (state.data.shortcuts.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.recently_used_actions),
                                    style = MaterialTheme.typography.titleSmall,
                                )

                                Spacer(Modifier.height(8.dp))

                                ShortcutRow(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth(),
                                    shortcuts = state.data.shortcuts,
                                    onClick = onClickShortcut,
                                )
                            }
                        }
                    }

                    is ConfigActionsState.Loaded -> {
                        Spacer(Modifier.height(8.dp))

                        ActionList(
                            modifier = Modifier.weight(1f),
                            actionList = state.data.actions,
                            shortcuts = state.data.shortcuts,
                            isReorderingEnabled = state.data.isReorderingEnabled,
                            onRemoveClick = onRemoveClick,
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
            contentType = { _, _ -> "key" },
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
                ShortcutRow(
                    modifier = Modifier.fillMaxWidth(),
                    shortcuts = shortcuts,
                    onClick = { onClickShortcut(it) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun EmptyPreview() {
    KeyMapperTheme {
        KeyMapActionsScreen(
            state = State.Data(
                ConfigActionsState.Empty(
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Toggle Back flashlight",
                            data = ActionData.Flashlight.Toggle(lens = CameraLens.BACK),
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
        KeyMapActionsScreen(
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
                            linkType = LinkType.PLUS,
                        ),
                    ),
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Toggle Back flashlight",
                            data = ActionData.Flashlight.Toggle(lens = CameraLens.BACK),
                        ),
                    ),
                    isReorderingEnabled = true,
                ),
            ),
        )
    }
}
