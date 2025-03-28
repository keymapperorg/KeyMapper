package io.github.sds100.keymapper.mappings.keymaps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import io.github.sds100.keymapper.util.ui.compose.CustomDialog

@Composable
fun CreateKeyMapShortcutScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateKeyMapShortcutViewModel,
    finishActivity: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateKeyMapShortcutScreen(
        modifier = modifier,
        listItems = state.listItems,
        showShortcutNameDialog = viewModel.showShortcutNameDialog,
        dismissShortcutNameDialog = { viewModel.showShortcutNameDialog = null },
        onShortcutNameResult = { name ->
            viewModel.shortcutNameDialogResult.value = name
            viewModel.showShortcutNameDialog = null
        },
        onClickKeyMap = viewModel::onKeyMapCardClick,
        finishActivity = finishActivity,

    )
}

@Composable
private fun CreateKeyMapShortcutScreen(
    modifier: Modifier = Modifier,
    listItems: State<List<KeyMapListItemModel>>,
    onClickKeyMap: (String) -> Unit = {},
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

    // TODO allow navigating between groups and hide the FAB.
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { showBackDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.bottom_app_bar_back_content_description),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.caption_create_keymap_shortcut),
            )

            KeyMapList(
                modifier = Modifier.fillMaxSize(),
                footerText = stringResource(R.string.create_key_map_shortcut_footer),
                listItems = listItems,
                isSelectable = false,
                onClickKeyMap = onClickKeyMap,
            )
        }
    }

    BackHandler {
        showBackDialog = true
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

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        CreateKeyMapShortcutScreen(
            listItems = State.Data(sampleList()),
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
