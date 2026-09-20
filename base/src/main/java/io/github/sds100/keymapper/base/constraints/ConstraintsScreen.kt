package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.keymaps.ShortcutModel
import io.github.sds100.keymapper.base.keymaps.ShortcutRow
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.DraggableItem
import io.github.sds100.keymapper.base.utils.ui.compose.TextFieldDialog
import io.github.sds100.keymapper.base.utils.ui.compose.rememberDragDropState
import io.github.sds100.keymapper.base.utils.ui.drawable
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.camera.CameraLens

/**
 * The height of the row between group cards. The last card has a spacer of the same height so
 * the height of the items stays constant while dragging.
 */
private val linkRowHeight = 40.dp

@Composable
fun ConstraintsScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigConstraintsViewModel,
    snackbarHost: SnackbarHostState,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (viewModel.showDuplicateConstraintsSnackbar) {
        val message = stringResource(R.string.error_duplicate_constraint)

        LaunchedEffect(viewModel.showDuplicateConstraintsSnackbar) {
            snackbarHost.showSnackbar(message)
            viewModel.showDuplicateConstraintsSnackbar = false
        }
    }

    ConstraintsScreen(
        modifier = modifier,
        state = state,
        onAddClick = { viewModel.addConstraint(groupUid = null) },
        onAddToGroupClick = viewModel::addConstraint,
        onRemoveClick = viewModel::onRemoveClick,
        onRemoveGroupClick = viewModel::onRemoveGroupClick,
        onNotClick = viewModel::onNotClick,
        onFixErrorClick = viewModel::onFixError,
        onClickShortcut = viewModel::onClickShortcut,
        onSelectMode = viewModel::onSelectMode,
        onSelectGroupMode = viewModel::onSelectGroupMode,
        onRenameGroup = viewModel::onRenameGroup,
        onExpandedChange = viewModel::onExpandedChange,
        onMoveGroup = viewModel::onMoveGroup,
    )
}

@Composable
private fun ConstraintsScreen(
    modifier: Modifier = Modifier,
    state: State<ConfigConstraintsState>,
    onAddClick: () -> Unit = {},
    onAddToGroupClick: (String) -> Unit = {},
    onRemoveClick: (String) -> Unit = {},
    onRemoveGroupClick: (String) -> Unit = {},
    onNotClick: (String) -> Unit = {},
    onFixErrorClick: (String) -> Unit = {},
    onClickShortcut: (ConstraintData) -> Unit = {},
    onSelectMode: (ConstraintMode) -> Unit = {},
    onSelectGroupMode: (String, ConstraintMode) -> Unit = { _, _ -> },
    onRenameGroup: (groupUid: String, name: String) -> Unit = { _, _ -> },
    onExpandedChange: (groupUid: String, expanded: Boolean) -> Unit = { _, _ -> },
    onMoveGroup: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
) {
    var constraintToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var groupToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var groupToRename by rememberSaveable { mutableStateOf<String?>(null) }

    if (constraintToDelete != null) {
        AlertDialog(
            onDismissRequest = { constraintToDelete = null },
            title = {
                Text(stringResource(R.string.constraint_list_delete_dialog_title))
            },
            text = { Text(stringResource(R.string.constraint_list_delete_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveClick(constraintToDelete!!)
                        constraintToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.constraint_list_delete_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { constraintToDelete = null }) {
                    Text(stringResource(R.string.constraint_list_delete_cancel))
                }
            },
        )
    }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = {
                Text(stringResource(R.string.constraint_group_delete_dialog_title))
            },
            text = { Text(stringResource(R.string.constraint_group_delete_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveGroupClick(groupToDelete!!)
                        groupToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.constraint_list_delete_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text(stringResource(R.string.constraint_list_delete_cancel))
                }
            },
        )
    }

    val groups = ((state as? State.Data)?.data as? ConfigConstraintsState.Loaded)
        ?.groups
        .orEmpty()
    val renameModel = groups.find { it.uid == groupToRename }

    if (renameModel != null) {
        TextFieldDialog(
            title = stringResource(R.string.constraint_group_rename_hint),
            submitButtonText = stringResource(R.string.pos_save),
            initialText = renameModel.name.orEmpty(),
            canBeEmpty = true,
            onSubmitClick = { newText ->
                onRenameGroup(renameModel.uid, newText)
                null
            },
            onDismissRequest = { groupToRename = null },
        )
    }

    when (state) {
        State.Loading -> Loading()

        is State.Data<ConfigConstraintsState> -> Surface(modifier = modifier) {
            Column {
                when (val data = state.data) {
                    is ConfigConstraintsState.Empty -> {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth(),
                                text = stringResource(
                                    R.string.constraints_recyclerview_placeholder,
                                ),
                                textAlign = TextAlign.Center,
                            )

                            if (data.shortcuts.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.recently_used_constraints),
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

                    is ConfigConstraintsState.Loaded -> {
                        ConstraintGroupList(
                            modifier = Modifier.weight(1f),
                            state = data,
                            onAddToGroupClick = onAddToGroupClick,
                            onRemoveClick = { constraintToDelete = it },
                            onRemoveGroupClick = { groupToDelete = it },
                            onNotClick = onNotClick,
                            onFixErrorClick = onFixErrorClick,
                            onClickShortcut = onClickShortcut,
                            onSelectGroupMode = onSelectGroupMode,
                            onRenameGroupClick = { groupToRename = it },
                            onExpandedChange = onExpandedChange,
                            onMoveGroup = onMoveGroup,
                        )

                        if (data.groups.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(R.string.constraint_groups_mode_title),
                                    style = MaterialTheme.typography.titleSmall,
                                )

                                ConstraintModeButtons(
                                    modifier = Modifier.width(160.dp),
                                    mode = data.mode,
                                    onSelectMode = onSelectMode,
                                )
                            }
                        }
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
                    Text(stringResource(R.string.button_add_constraint))
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
private fun ConstraintGroupList(
    modifier: Modifier = Modifier,
    state: ConfigConstraintsState.Loaded,
    onAddToGroupClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    onRemoveGroupClick: (String) -> Unit,
    onNotClick: (String) -> Unit,
    onFixErrorClick: (String) -> Unit,
    onClickShortcut: (ConstraintData) -> Unit,
    onSelectGroupMode: (String, ConstraintMode) -> Unit,
    onRenameGroupClick: (groupUid: String) -> Unit,
    onExpandedChange: (groupUid: String, expanded: Boolean) -> Unit,
    onMoveGroup: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val groupUids = state.groups.map { it.uid }

    // Only the groups can be dragged. Not the row of shortcuts.
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        keys = groupUids,
        onMove = onMoveGroup,
    )

    val orderedGroups = dragDropState.ordered(state.groups) { it.uid }

    val linkText = when (state.mode) {
        ConstraintMode.AND -> stringResource(R.string.constraint_mode_and)
        ConstraintMode.OR -> stringResource(R.string.constraint_mode_or)
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item(key = "header") {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                text = stringResource(R.string.constraint_list_explanation),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
        }

        itemsIndexed(
            orderedGroups,
            key = { _, group -> group.uid },
            contentType = { _, _ -> "constraint_group" },
        ) { index, group ->
            DraggableItem(
                dragDropState = dragDropState,
                key = group.uid,
            ) { isDragging ->
                Column {
                    ConstraintGroupItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        model = group,
                        isExpanded = group.isExpanded,
                        isDraggingEnabled = orderedGroups.size > 1,
                        isDragging = isDragging,
                        isReorderingEnabled = state.groups.size > 1,
                        dragDropState = dragDropState,
                        onExpandedChange = { expanded -> onExpandedChange(group.uid, expanded) },
                        onSelectMode = { onSelectGroupMode(group.uid, it) },
                        onAddConstraintClick = { onAddToGroupClick(group.uid) },
                        onRenameClick = { onRenameGroupClick(group.uid) },
                        onDeleteClick = { onRemoveGroupClick(group.uid) },
                        onRemoveConstraintClick = onRemoveClick,
                        onFixConstraintClick = onFixErrorClick,
                        onNotClick = onNotClick,
                        onMoveUp = if (index > 0) {
                            { onMoveGroup(index, index - 1) }
                        } else {
                            null
                        },
                        onMoveDown = if (index < orderedGroups.lastIndex) {
                            { onMoveGroup(index, index + 1) }
                        } else {
                            null
                        },
                    )

                    if (index < orderedGroups.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(linkRowHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = linkText,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        // Important! Keep the height of the item constant while dragging.
                        // If the height changes while dragging it can lead to janky behavior.
                        Spacer(Modifier.height(linkRowHeight))
                    }
                }
            }
        }

        if (state.shortcuts.isNotEmpty()) {
            item(key = "shortcuts", contentType = "shortcuts") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.recently_used_constraints),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    ShortcutRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        shortcuts = state.shortcuts,
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
        ConstraintsScreen(
            state = State.Data(
                ConfigConstraintsState.Empty(
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Flashlight is on",
                            data = ConstraintData.FlashlightOn(lens = CameraLens.BACK),
                        ),
                    ),
                ),
            ),
        )
    }
}

@Preview(heightDp = 1200)
@Composable
private fun LoadedPreview() {
    KeyMapperTheme {
        val ctx = LocalContext.current

        val flashlightConstraint = ConstraintListItemModel(
            id = "1",
            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
            text = "Flashlight is on",
            isNot = true,
            error = "Flashlight not found",
            isErrorFixable = true,
        )

        val appConstraint = ConstraintListItemModel(
            id = "2",
            icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
            text = "Key Mapper is in foreground",
        )

        val wifiConstraint = ConstraintListItemModel(
            id = "3",
            icon = ComposeIconInfo.Vector(Icons.Outlined.Wifi),
            text = "Wi-Fi is on",
        )

        ConstraintsScreen(
            state = State.Data(
                ConfigConstraintsState.Loaded(
                    groups = listOf(
                        ConstraintGroupListItemModel(
                            uid = "group1",
                            mode = ConstraintMode.AND,
                            constraints = listOf(flashlightConstraint, appConstraint),
                            description = "Flashlight is not on AND Key Mapper is in foreground",
                        ),
                        ConstraintGroupListItemModel(
                            uid = "group2",
                            name = "Foreground or wifi",
                            mode = ConstraintMode.OR,
                            constraints = listOf(appConstraint, wifiConstraint),
                            description = "Key Mapper is in foreground OR Wi-Fi is on",
                        ),
                        ConstraintGroupListItemModel(
                            uid = "group3",
                            mode = ConstraintMode.AND,
                            constraints = listOf(wifiConstraint),
                            description = "Wi-Fi is on",
                        ),
                    ),
                    mode = ConstraintMode.OR,
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Flashlight is on",
                            data = ConstraintData.FlashlightOn(lens = CameraLens.BACK),
                        ),
                    ),
                ),
            ),
        )
    }
}
