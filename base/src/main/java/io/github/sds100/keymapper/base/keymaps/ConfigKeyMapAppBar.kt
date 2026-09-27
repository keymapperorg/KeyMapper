package io.github.sds100.keymapper.base.keymaps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.EditableAppBarTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigKeyMapAppBar(
    modifier: Modifier = Modifier,
    name: TextFieldValue,
    onNameChange: (TextFieldValue) -> Unit,
    isEditingName: Boolean,
    onEditNameClick: () -> Unit,
    onConfirmNameClick: () -> Unit,
    isKeyMapEnabled: Boolean,
    onKeyMapEnabledChange: (Boolean) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    BoxWithConstraints(modifier) {
        val showUndoRedoInline = maxWidth >= 400.dp

        BottomAppBar(
            modifier = Modifier.imePadding(),
            actions = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.action_go_back),
                    )
                }

                EditableAppBarTextField(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = stringResource(R.string.config_key_map_name_placeholder),
                    isEditing = isEditingName,
                    onConfirmClick = onConfirmNameClick,
                    onEditClick = onEditNameClick,
                    confirmContentDescription = stringResource(
                        R.string.config_key_map_save_name,
                    ),
                    editContentDescription = stringResource(R.string.config_key_map_edit_name),
                )

                AnimatedVisibility(visible = !isEditingName) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showUndoRedoInline) {
                            IconButton(onClick = onUndoClick, enabled = canUndo) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Undo,
                                    stringResource(R.string.action_undo),
                                )
                            }

                            IconButton(onClick = onRedoClick, enabled = canRedo) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Redo,
                                    stringResource(R.string.action_redo),
                                )
                            }

                            Spacer(Modifier.width(8.dp))
                        }

                        val enabledText = if (isKeyMapEnabled) {
                            stringResource(R.string.switch_enabled)
                        } else {
                            stringResource(R.string.switch_disabled)
                        }

                        val tooltipText = if (isKeyMapEnabled) {
                            stringResource(R.string.config_key_map_enabled_tooltip)
                        } else {
                            stringResource(R.string.config_key_map_disabled_tooltip)
                        }

                        val tooltipState = rememberTooltipState()
                        val coroutineScope = rememberCoroutineScope()

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above,
                            ),
                            tooltip = { PlainTooltip { Text(tooltipText) } },
                            state = tooltipState,
                        ) {
                            Switch(
                                modifier = Modifier.semantics {
                                    contentDescription = enabledText
                                },
                                checked = isKeyMapEnabled,
                                onCheckedChange = {
                                    onKeyMapEnabledChange(it)
                                    coroutineScope.launch { tooltipState.show() }
                                },
                            )
                        }

                        if (!showUndoRedoInline) {
                            Spacer(Modifier.width(8.dp))

                            UndoRedoMenu(
                                canUndo = canUndo,
                                canRedo = canRedo,
                                onUndoClick = onUndoClick,
                                onRedoClick = onRedoClick,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun UndoRedoMenu(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.home_app_bar_more),
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        offset = DpOffset(x = 0.dp, y = 100.dp),
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null) },
            text = { Text(stringResource(R.string.action_undo)) },
            enabled = canUndo,
            onClick = {
                expanded = false
                onUndoClick()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = null) },
            text = { Text(stringResource(R.string.action_redo)) },
            enabled = canRedo,
            onClick = {
                expanded = false
                onRedoClick()
            },
        )
    }
}

@Composable
private fun PreviewAppBar(
    name: String = "",
    isEditingName: Boolean = false,
    isKeyMapEnabled: Boolean = true,
    canUndo: Boolean = true,
    canRedo: Boolean = true,
) {
    KeyMapperTheme {
        ConfigKeyMapAppBar(
            name = TextFieldValue(name),
            onNameChange = {},
            isEditingName = isEditingName,
            onEditNameClick = {},
            onConfirmNameClick = {},
            isKeyMapEnabled = isKeyMapEnabled,
            onKeyMapEnabledChange = {},
            canUndo = canUndo,
            canRedo = canRedo,
            onUndoClick = {},
            onRedoClick = {},
            onBackClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun UnnamedPreview() {
    PreviewAppBar(name = "")
}

@PreviewLightDark
@Composable
private fun EditingNamePreview() {
    PreviewAppBar(name = "Volume up to skip song", isEditingName = true)
}

@PreviewLightDark
@Composable
private fun EditingEmptyNamePreview() {
    PreviewAppBar(name = "", isEditingName = true)
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun NamedPreview() {
    PreviewAppBar(name = "Volume up to skip song")
}
