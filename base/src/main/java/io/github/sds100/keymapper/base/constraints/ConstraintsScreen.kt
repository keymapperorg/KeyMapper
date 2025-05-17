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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
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
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.keymaps.ShortcutModel
import io.github.sds100.keymapper.base.keymaps.ShortcutRow
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.common.util.state.State
import io.github.sds100.keymapper.base.util.drawable
import io.github.sds100.keymapper.base.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.util.ui.compose.RadioButtonText

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
        onRemoveClick = viewModel::onRemoveClick,
        onFixErrorClick = viewModel::onFixError,
        onClickShortcut = viewModel::onClickShortcut,
        onAddClick = viewModel::addConstraint,
        onSelectMode = viewModel::onSelectMode,
    )
}

@Composable
private fun ConstraintsScreen(
    modifier: Modifier = Modifier,
    state: State<ConfigConstraintsState>,
    onAddClick: () -> Unit = {},
    onRemoveClick: (String) -> Unit = {},
    onFixErrorClick: (String) -> Unit = {},
    onClickShortcut: (Constraint) -> Unit = {},
    onSelectMode: (ConstraintMode) -> Unit = {},
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var constraintToDelete by rememberSaveable { mutableStateOf<String?>(null) }

    if (showDeleteDialog && constraintToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(stringResource(R.string.constraint_list_delete_dialog_title))
            },
            text = { Text(stringResource(R.string.constraint_list_delete_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveClick(constraintToDelete!!)
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.constraint_list_delete_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.constraint_list_delete_cancel))
                }
            },
        )
    }

    when (state) {
        State.Loading -> Loading()
        is State.Data<ConfigConstraintsState> -> Surface(modifier = modifier) {
            Column {
                when (state.data) {
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
                                text = stringResource(R.string.constraints_recyclerview_placeholder),
                                textAlign = TextAlign.Center,
                            )

                            if (state.data.shortcuts.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.recently_used_constraints),
                                    style = MaterialTheme.typography.titleSmall,
                                )

                                Spacer(Modifier.height(8.dp))

                                ShortcutRow(
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp)
                                        .fillMaxWidth(),
                                    shortcuts = state.data.shortcuts,
                                    onClick = onClickShortcut,
                                )
                            }
                        }
                    }

                    is ConfigConstraintsState.Loaded -> {
                        Spacer(Modifier.height(8.dp))

                        if (state.data.constraintList.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = stringResource(R.string.constraint_list_explanation_header),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        ConstraintList(
                            modifier = Modifier.weight(1f),
                            constraintList = state.data.constraintList,
                            shortcuts = state.data.shortcuts,
                            onRemoveClick = {
                                constraintToDelete = it
                                showDeleteDialog = true
                            },
                            onFixErrorClick = onFixErrorClick,
                            onClickShortcut = onClickShortcut,
                        )

                        if (state.data.constraintList.size > 1) {
                            ConstraintModeRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                mode = state.data.selectedMode,
                                onSelectMode = onSelectMode,
                            )
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
private fun ConstraintModeRow(
    modifier: Modifier = Modifier,
    mode: ConstraintMode,
    onSelectMode: (ConstraintMode) -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(R.string.constraint_mode_title),
            style = MaterialTheme.typography.labelLarge,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            RadioButtonText(
                modifier = Modifier.weight(1f),
                isSelected = mode == ConstraintMode.AND,
                text = stringResource(R.string.constraint_mode_and),
                onSelected = { onSelectMode(ConstraintMode.AND) },
            )

            RadioButtonText(
                modifier = Modifier.weight(1f),
                isSelected = mode == ConstraintMode.OR,
                text = stringResource(R.string.constraint_mode_or),
                onSelected = { onSelectMode(ConstraintMode.OR) },
            )
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
private fun ConstraintList(
    modifier: Modifier = Modifier,
    constraintList: List<ConstraintListItemModel>,
    shortcuts: Set<ShortcutModel<Constraint>>,
    onRemoveClick: (String) -> Unit,
    onFixErrorClick: (String) -> Unit,
    onClickShortcut: (Constraint) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    // Use dragContainer rather than .draggable() modifier because that causes
    // dragging the first item to be always be dropped in the next position.
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            constraintList,
            key = { item -> item.id },
            contentType = { _ -> "constraint" },
        ) { model ->
            ConstraintListItem(
                modifier = Modifier.fillMaxWidth(),
                model = model,
                onRemoveClick = { onRemoveClick(model.id) },
                onFixClick = { onFixErrorClick(model.id) },
            )
        }

        if (shortcuts.isNotEmpty()) {
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
        ConstraintsScreen(
            state = State.Data(
                ConfigConstraintsState.Empty(
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Flashlight is on",
                            data = Constraint.FlashlightOn(lens = CameraLens.BACK),
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
        val ctx = LocalContext.current

        ConstraintsScreen(
            state = State.Data(
                ConfigConstraintsState.Loaded(
                    constraintList = listOf(
                        ConstraintListItemModel(
                            id = "1",
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            constraintModeLink = ConstraintMode.AND,
                            text = "Flashlight is on",
                            error = "Flashlight not found",
                            isErrorFixable = true,
                        ),
                        ConstraintListItemModel(
                            id = "2",
                            icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
                            constraintModeLink = null,
                            text = "Key Mapper in foreground",
                            error = null,
                            isErrorFixable = true,
                        ),
                    ),
                    shortcuts = setOf(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.FlashlightOn),
                            text = "Flashlight is on",
                            data = Constraint.FlashlightOn(lens = CameraLens.BACK),
                        ),
                    ),
                    selectedMode = ConstraintMode.AND,
                ),
            ),
        )
    }
}
