package io.github.sds100.keymapper.mapping.keymaps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.groups.GroupBreadcrumbRow
import io.github.sds100.keymapper.groups.GroupListItemModel
import io.github.sds100.keymapper.groups.GroupRow
import io.github.sds100.keymapper.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.trigger.TriggerError
import io.github.sds100.keymapper.common.util.state.State
import io.github.sds100.keymapper.base.util.drawable
import io.github.sds100.keymapper.base.util.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.util.ui.compose.CustomDialog

@Composable
fun CreateKeyMapShortcutScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateKeyMapShortcutViewModel,
    finishActivity: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateKeyMapShortcutScreen(
        modifier = modifier,
        state = state,
        showShortcutNameDialog = viewModel.showShortcutNameDialog,
        dismissShortcutNameDialog = { viewModel.showShortcutNameDialog = null },
        onShortcutNameResult = { name ->
            viewModel.shortcutNameDialogResult.value = name
            viewModel.showShortcutNameDialog = null
        },
        onKeyMapClick = viewModel::onKeyMapCardClick,
        onGroupClick = viewModel::onGroupClick,
        onPopGroupClick = viewModel::onPopGroupClick,
        finishActivity = finishActivity,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateKeyMapShortcutScreen(
    modifier: Modifier = Modifier,
    state: KeyMapListState,
    onKeyMapClick: (String) -> Unit = {},
    onGroupClick: (String?) -> Unit = {},
    onPopGroupClick: () -> Unit = {},
    finishActivity: () -> Unit = {},
    showShortcutNameDialog: String?,
    dismissShortcutNameDialog: () -> Unit = {},
    onShortcutNameResult: (String) -> Unit = {},
) {
    var showBackDialog by rememberSaveable { mutableStateOf(false) }

    if (showBackDialog) {
        BackDialog(
            onDismiss = { showBackDialog = false },
            onDiscardClick = finishActivity,
        )
    }

    if (showShortcutNameDialog != null) {
        ShortcutNameDialog(
            onSaveClick = onShortcutNameResult,
            onDismissRequest = dismissShortcutNameDialog,
            initialText = showShortcutNameDialog,
        )
    }

    BackHandler { showBackDialog = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedContent(state.appBarState, contentKey = { it::class }) { state ->
                when (state) {
                    is KeyMapAppBarState.RootGroup ->
                        Column(modifier) {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(stringResource(R.string.create_key_map_shortcut_app_title))
                                },
                                navigationIcon = {
                                    IconButton(onClick = { showBackDialog = true }) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = stringResource(R.string.bottom_app_bar_back_content_description),
                                        )
                                    }
                                },
                            )

                            GroupRow(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxWidth(),
                                groups = state.subGroups,
                                showNewGroup = false,
                                onGroupClick = onGroupClick,
                                isSubgroups = false,
                            )
                        }

                    is KeyMapAppBarState.ChildGroup -> {
                        Column {
                            TopAppBar(
                                title = {
                                    Text(
                                        state.groupName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = onPopGroupClick) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = stringResource(R.string.home_app_bar_pop_group),
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )

                            GroupBreadcrumbRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                groups = state.breadcrumbs,
                                onGroupClick = onGroupClick,
                            )

                            GroupRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                groups = state.subGroups,
                                showNewGroup = false,
                                onGroupClick = onGroupClick,
                                enabled = true,
                                isSubgroups = true,
                            )
                        }
                    }

                    else -> {}
                }
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            KeyMapList(
                modifier = Modifier.fillMaxSize(),
                footerText = stringResource(R.string.create_key_map_shortcut_footer),
                listItems = state.listItems,
                isSelectable = false,
                onClickKeyMap = onKeyMapClick,
            )
        }
    }
}

@Composable
private fun BackDialog(
    onDismiss: () -> Unit,
    onDiscardClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_unsaved_changes)) },
        text = { Text(stringResource(R.string.dialog_message_unsaved_changes)) },
        confirmButton = {
            TextButton(onClick = onDiscardClick) { Text(stringResource(R.string.pos_discard_changes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.neg_keep_editing)) }
        },
    )
}

@Composable
private fun ShortcutNameDialog(
    onSaveClick: (name: String) -> Unit = { },
    onDismissRequest: () -> Unit = {},
    initialText: String = "",
) {
    var newName by rememberSaveable { mutableStateOf(initialText) }
    val isError by remember { derivedStateOf { newName.isBlank() } }

    CustomDialog(
        title = stringResource(R.string.hint_shortcut_name),
        confirmButton = {
            TextButton(onClick = {
                onSaveClick(newName)
            }, enabled = !isError) {
                Text(stringResource(R.string.pos_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = newName,
            onValueChange = {
                newName = it
            },
            singleLine = true,
            maxLines = 1,
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(stringResource(R.string.error_cant_be_empty))
                }
            },
        )
    }
}

@Composable
private fun keyMapSampleList(): List<KeyMapListItemModel> {
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
                        error = Error.AppNotFound("io.github.sds100.keymapper"),
                    ),
                ),
                options = listOf("Vibrate"),
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
                options = emptyList(),
                triggerErrors = emptyList(),
                extraInfo = "Disabled • No trigger",
            ),
        ),
    )
}

@Composable
private fun constraintsSampleList(): List<ComposeChipModel> {
    val ctx = LocalContext.current

    return listOf(
        ComposeChipModel.Normal(
            id = "1",
            text = "Device is locked",
            icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
        ),
        ComposeChipModel.Normal(
            id = "2",
            text = "Key Mapper is open",
            icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
        ),
        ComposeChipModel.Error(
            id = "2",
            text = "Key Mapper not found",
            error = Error.AppNotFound("io.github.sds100.keymapper"),
        ),
    )
}

@Composable
private fun groupSampleList(): List<GroupListItemModel> {
    val ctx = LocalContext.current

    return listOf(
        GroupListItemModel(
            uid = "1",
            name = "Lockscreen",
            icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
        ),
        GroupListItemModel(
            uid = "2",
            name = "Key Mapper",
            icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
        ),
        GroupListItemModel(
            uid = "3",
            name = "Key Mapper",
            icon = null,
        ),
    )
}

@Preview
@Composable
private fun PreviewRootGroup() {
    KeyMapperTheme {
        CreateKeyMapShortcutScreen(
            state = KeyMapListState(
                appBarState = KeyMapAppBarState.RootGroup(
                    subGroups = groupSampleList(),
                    warnings = emptyList(),
                    isPaused = true,
                ),
                listItems = State.Data(keyMapSampleList()),
            ),
            showShortcutNameDialog = null,
        )
    }
}

@Preview
@Composable
private fun PreviewChildGroup() {
    KeyMapperTheme {
        CreateKeyMapShortcutScreen(
            state = KeyMapListState(
                appBarState = KeyMapAppBarState.ChildGroup(
                    groupName = "Very very very very very long name",
                    subGroups = groupSampleList(),
                    constraints = constraintsSampleList(),
                    parentConstraintCount = 1,
                    constraintMode = ConstraintMode.AND,
                    breadcrumbs = groupSampleList(),
                    isEditingGroupName = false,
                    isNewGroup = false,
                ),
                listItems = State.Data(keyMapSampleList()),
            ),
            showShortcutNameDialog = null,
        )
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        CreateKeyMapShortcutScreen(
            state = KeyMapListState(
                appBarState = KeyMapAppBarState.RootGroup(
                    subGroups = emptyList(),
                    warnings = emptyList(),
                    isPaused = true,
                ),
                listItems = State.Data(emptyList()),
            ),
            showShortcutNameDialog = null,
        )
    }
}

@Preview
@Composable
private fun BackDialogPreview() {
    KeyMapperTheme {
        BackDialog(onDismiss = {}, onDiscardClick = {})
    }
}

@Preview
@Composable
private fun ShortcutNameDialogPreview() {
    KeyMapperTheme {
        ShortcutNameDialog()
    }
}
