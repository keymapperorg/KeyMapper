package io.github.sds100.keymapper.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.groups.GroupRow
import io.github.sds100.keymapper.groups.SubGroupListModel
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAppBarState
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.compose.icons.Import
import io.github.sds100.keymapper.util.ui.compose.icons.KeyMapperIcons
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun KeyMapAppBar(
    modifier: Modifier = Modifier,
    state: KeyMapAppBarState,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onSortClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onTogglePausedClick: () -> Unit = {},
    onFixWarningClick: (String) -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSelectAllClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onNewGroupClick: () -> Unit = {},
    onRenameGroupClick: suspend (String) -> Boolean = { true },
    isEditingGroupName: Boolean = false,
    onEditGroupNameClick: () -> Unit = {},
) {
    BackHandler(onBack = onBackClick)

    when (state) {
        is KeyMapAppBarState.RootGroup -> RootGroupAppBar(
            modifier = modifier,
            state = state,
            scrollBehavior = scrollBehavior,
            onTogglePausedClick = onTogglePausedClick,
            onSortClick = onSortClick,
            onFixWarningClick = onFixWarningClick,
            onNewGroupClick = onNewGroupClick,
            actions = {
                AnimatedVisibility(!isEditingGroupName) {
                    AppBarActions(
                        state,
                        onSelectAllClick,
                        onHelpClick,
                        onSettingsClick,
                        onAboutClick,
                        onExportClick,
                        onImportClick,
                    )
                }
            },
        )

        is KeyMapAppBarState.Selecting -> SelectingAppBar(
            modifier = modifier,
            state = state,
            onBackClick = onBackClick,
            onSelectAllClick = onSelectAllClick,
        )

        is KeyMapAppBarState.ChildGroup -> {
            val scope = rememberCoroutineScope()
            val uniqueErrorText = stringResource(R.string.home_app_bar_group_name_unique_error)

            var error: String? by rememberSaveable { mutableStateOf(null) }

            var newName by remember { mutableStateOf(state.groupName) }

            ChildGroupAppBar(
                modifier = modifier,
                value = newName,
                placeholder = state.groupName,
                error = error,
                onValueChange = {
                    newName = it
                    error = null
                },
                onRenameClick = {
                    scope.launch {
                        if (!onRenameGroupClick(newName)) {
                            error = uniqueErrorText
                        }
                    }
                },
                onBackClick = onBackClick,
                onNewGroupClick = onNewGroupClick,
                onEditClick = onEditGroupNameClick,
                isEditingGroupName = isEditingGroupName,
                actions = {
                    AnimatedVisibility(!isEditingGroupName) {
                        AppBarActions(
                            state,
                            onSelectAllClick,
                            onHelpClick,
                            onSettingsClick,
                            onAboutClick,
                            onExportClick,
                            onImportClick,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun primaryAppBarColors(): TopAppBarColors {
    return TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootGroupAppBar(
    modifier: Modifier = Modifier,
    state: KeyMapAppBarState.RootGroup,
    scrollBehavior: TopAppBarScrollBehavior,
    onTogglePausedClick: () -> Unit,
    onSortClick: () -> Unit,
    onFixWarningClick: (String) -> Unit,
    onNewGroupClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    // This is taken from the AppBar color code.
    val colorTransitionFraction by
        remember(scrollBehavior) {
            // derivedStateOf to prevent redundant recompositions when the content scrolls.
            derivedStateOf {
                val overlappingFraction = scrollBehavior.state.overlappedFraction
                if (overlappingFraction > 0.01f) 1f else 0f
            }
        }

    val appBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors()

    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            appBarColors.containerColor,
            appBarColors.scrolledContainerColor,
            FastOutLinearInEasing.transform(colorTransitionFraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    Column(modifier) {
        CenterAlignedTopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                AppBarStatus(
                    isPaused = state.isPaused,
                    warnings = state.warnings,
                    onTogglePausedClick = onTogglePausedClick,
                )
            },
            navigationIcon = {
                IconButton(onClick = onSortClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = stringResource(R.string.home_app_bar_sort),
                    )
                }
            },
            actions = actions,
            colors = appBarColors,
        )

        AnimatedVisibility(visible = state.warnings.isNotEmpty()) {
            // Use separate Surfaces so the animation doesn't jump when they both disappear
            // going into selection mode.
            Surface(color = appBarContainerColor) {
                HomeWarningList(
                    modifier = Modifier.padding(bottom = 8.dp),
                    warnings = (state as? KeyMapAppBarState.RootGroup)?.warnings ?: emptyList(),
                    onFixClick = onFixWarningClick,
                )
            }
        }

        Surface(color = appBarContainerColor) {
            GroupRow(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                groups = state.subGroups,
                onNewGroupClick = onNewGroupClick,
                onGroupClick = {},
            )
        }
    }
}

@Composable
private fun ChildGroupAppBar(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit = {},
    error: String? = null,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    isEditingGroupName: Boolean = false,
    onNewGroupClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp)
                .height(intrinsicSize = IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.home_app_bar_pop_group),
                )
            }

            Spacer(Modifier.width(8.dp))

            GroupNameRow(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                onRenameClick = onRenameClick,
                error = error,
                isEditing = isEditingGroupName,
                onEditClick = onEditClick,
            )

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(visible = !isEditingGroupName) {
                actions()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectingAppBar(
    modifier: Modifier = Modifier,
    state: KeyMapAppBarState.Selecting,
    onBackClick: () -> Unit,
    onSelectAllClick: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            SelectedText(selectionCount = state.selectionCount)
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.home_app_bar_cancel_selecting),
                )
            }
        },
        actions = {
            OutlinedButton(
                modifier = Modifier.padding(horizontal = 8.dp),
                onClick = onSelectAllClick,
            ) {
                val text = if (state.isAllSelected) {
                    stringResource(R.string.home_app_bar_deselect_all)
                } else {
                    stringResource(R.string.home_app_bar_select_all)
                }
                Text(text)
            }
        },
        colors = primaryAppBarColors(),
    )
}

@Composable
private fun AppBarActions(
    state: KeyMapAppBarState,
    onSelectAllClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    var expandedDropdown by rememberSaveable { mutableStateOf(false) }

    AnimatedContent(state is KeyMapAppBarState.Selecting) { isSelecting ->
        if (isSelecting && state is KeyMapAppBarState.Selecting) {
            OutlinedButton(
                modifier = Modifier.padding(horizontal = 8.dp),
                onClick = onSelectAllClick,
            ) {
                val text = if (state.isAllSelected) {
                    stringResource(R.string.home_app_bar_deselect_all)
                } else {
                    stringResource(R.string.home_app_bar_select_all)
                }
                Text(text)
            }
        } else {
            Row {
                IconButton(onClick = onHelpClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.home_app_bar_help),
                    )
                }

                IconButton(onClick = { expandedDropdown = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.home_app_bar_more),
                    )
                }

                AppBarDropdownMenu(
                    expanded = expandedDropdown,
                    onSettingsClick = {
                        expandedDropdown = false
                        onSettingsClick()
                    },
                    onAboutClick = {
                        expandedDropdown = false
                        onAboutClick()
                    },
                    onExportClick = {
                        expandedDropdown = false
                        onExportClick()
                    },
                    onImportClick = {
                        expandedDropdown = false
                        onImportClick()
                    },
                    onDismissRequest = { expandedDropdown = false },
                )
            }
        }
    }
}

@Composable
private fun GroupNameRow(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit = {},
    placeholder: String,
    isEditing: Boolean,
    onRenameClick: () -> Unit,
    onEditClick: () -> Unit = {},
    error: String? = null,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        focusRequester.requestFocus()
    }

    AnimatedContent(isEditing) { isEditing ->
        Row(modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
            val interactionSource = remember { MutableInteractionSource() }

            // TODO handle error squishing the text field

            BasicTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .height(IntrinsicSize.Max)
                    .then(
                        if (isEditing) {
                            Modifier.weight(1f)
                        } else {
                            Modifier
                        },
                    ),
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.titleLarge,
                enabled = isEditing,
                keyboardActions = KeyboardActions(onDone = { onRenameClick() }),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    showKeyboardOnFocus = true,
                ),
                singleLine = true,
                maxLines = 1,
                interactionSource = interactionSource,
            ) { innerTextField ->
                @OptIn(ExperimentalMaterial3Api::class)
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    placeholder = {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    innerTextField = {
                        Box(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .height(48.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { innerTextField() }
                    },
                    singleLine = true,
                    colors = if (isEditing) {
                        OutlinedTextFieldDefaults.colors()
                    } else {
                        OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    },
                    isError = error != null,
                    enabled = isEditing,
                    supportingText = if (error == null) {
                        null
                    } else {
                        { Text(error, maxLines = 1) }
                    },
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                        top = 0.dp,
                        bottom = 0.dp,
                    ),
                )
            }

            if (isEditing) {
                IconButton(onClick = onRenameClick) {
                    Icon(
                        Icons.Rounded.Done,
                        contentDescription = stringResource(R.string.home_app_bar_save_group_name),
                    )
                }
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.home_app_bar_edit_group_name),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBarStatus(
    isPaused: Boolean,
    warnings: List<HomeWarningListItem>,
    onTogglePausedClick: () -> Unit,
) {
    val pausedButtonContainerColor by animateColorAsState(
        targetValue = if (isPaused || warnings.isNotEmpty()) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            LocalCustomColorsPalette.current.greenContainer
        },
    )

    val pausedButtonContentColor by animateColorAsState(
        targetValue = if (isPaused || warnings.isNotEmpty()) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            LocalCustomColorsPalette.current.onGreenContainer
        },
    )

    FilledTonalButton(
        modifier = Modifier.widthIn(min = 8.dp),
        onClick = onTogglePausedClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = pausedButtonContainerColor,
            contentColor = pausedButtonContentColor,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        val buttonIcon: ImageVector
        val buttonText: String

        if (isPaused) {
            buttonIcon = Icons.Rounded.PauseCircleOutline
            buttonText = stringResource(R.string.home_app_bar_status_paused)
        } else if (warnings.isNotEmpty()) {
            buttonIcon = Icons.Rounded.ErrorOutline
            buttonText = pluralStringResource(
                R.plurals.home_app_bar_status_warnings,
                warnings.size,
                warnings.size,
            )
        } else {
            buttonIcon = Icons.Rounded.PlayCircleOutline
            buttonText = stringResource(R.string.home_app_bar_status_running)
        }

        val transition =
            slideInVertically { height -> -height } + fadeIn() togetherWith slideOutVertically { height -> height } + fadeOut()

        AnimatedContent(targetState = buttonIcon, transitionSpec = { transition }) { icon ->
            Icon(icon, contentDescription = null)
        }

        AnimatedContent(
            targetState = buttonText,
            transitionSpec = { transition },
        ) { text ->
            Row {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text)
            }
        }
    }
}

@Composable
private fun SelectedText(modifier: Modifier = Modifier, selectionCount: Int) {
    Row(modifier) {
        AnimatedContent(
            selectionCount,
            transitionSpec = {
                selectedTextTransition(
                    targetState,
                    initialState,
                )
            },
        ) { selectionCount ->
            Text(selectionCount.toString())
        }

        Spacer(Modifier.width(4.dp))

        Text(stringResource(R.string.selection_count))
    }
}

private fun selectedTextTransition(
    targetState: Int,
    initialState: Int,
): ContentTransform {
    return slideInVertically { height ->
        if (targetState > initialState) {
            -height
        } else {
            height
        }
    } + fadeIn() togetherWith slideOutVertically { height ->
        if (targetState > initialState) {
            height
        } else {
            -height
        }
    } + fadeOut()
}

@Composable
private fun AppBarDropdownMenu(
    expanded: Boolean,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_settings)) },
            onClick = onSettingsClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.IosShare, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_export)) },
            onClick = onExportClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(KeyMapperIcons.Import, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_import)) },
            onClick = onImportClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_about)) },
            onClick = onAboutClick,
        )
    }
}

@Composable
private fun constraintsSampleList(): List<ComposeChipModel> {
    val context = LocalContext.current

    return listOf(
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
    )
}

@Composable
private fun groupSampleList(): List<SubGroupListModel> {
    val ctx = LocalContext.current

    return listOf(
        SubGroupListModel(
            uid = "0",
            name = "Key Mapper",
            icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun KeyMapsChildGroupPreview() {
    val state = KeyMapAppBarState.ChildGroup(
        groupName = "Short name",
        subGroups = groupSampleList(),
        constraints = constraintsSampleList(),
        constraintMode = ConstraintMode.AND,
    )
    KeyMapperTheme {
        KeyMapAppBar(modifier = Modifier.fillMaxWidth(), state = state, isEditingGroupName = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun KeyMapsChildGroupEditingPreview() {
    val state = KeyMapAppBarState.ChildGroup(
        groupName = "Untitled group 23",
        subGroups = groupSampleList(),
        constraints = constraintsSampleList(),
        constraintMode = ConstraintMode.AND,
    )

    val focusRequester = FocusRequester()

    LaunchedEffect("") {
        focusRequester.requestFocus()
    }

    KeyMapperTheme {
        KeyMapAppBar(
            state = state,
            isEditingGroupName = true,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun KeyMapsChildGroupErrorPreview() {
    val focusRequester = FocusRequester()

    LaunchedEffect("") {
        focusRequester.requestFocus()
    }

    KeyMapperTheme {
        ChildGroupAppBar(
            value = "Untitled group 23",
            placeholder = "Untitled group 23",
            error = stringResource(R.string.home_app_bar_group_name_unique_error),
            isEditingGroupName = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun KeyMapsRunningPreview() {
    val state = KeyMapAppBarState.RootGroup(
        subGroups = emptyList(),
        warnings = emptyList(),
        isPaused = false,
    )
    KeyMapperTheme {
        KeyMapAppBar(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStatePausedPreview() {
    val state = KeyMapAppBarState.RootGroup(
        subGroups = emptyList(),
        warnings = emptyList(),
        isPaused = true,
    )
    KeyMapperTheme {
        KeyMapAppBar(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStateWarningsPreview() {
    val warnings = listOf(
        HomeWarningListItem(
            id = "0",
            text = stringResource(R.string.home_error_accessibility_service_is_disabled),
        ),
        HomeWarningListItem(
            id = "1",
            text = stringResource(R.string.home_error_is_battery_optimised),
        ),
    )

    val state =
        KeyMapAppBarState.RootGroup(
            subGroups = emptyList(),
            warnings = warnings,
            isPaused = true,
        )
    KeyMapperTheme {
        KeyMapAppBar(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStateWarningsDarkPreview() {
    val warnings = listOf(
        HomeWarningListItem(
            id = "0",
            text = stringResource(R.string.home_error_accessibility_service_is_disabled),
        ),
        HomeWarningListItem(
            id = "1",
            text = stringResource(R.string.home_error_is_battery_optimised),
        ),
    )

    val state =
        KeyMapAppBarState.RootGroup(
            subGroups = emptyList(),
            warnings = warnings,
            isPaused = true,
        )
    KeyMapperTheme {
        KeyMapAppBar(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun HomeStateSelectingPreview() {
    val state = KeyMapAppBarState.Selecting(
        selectionCount = 4,
        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
        isAllSelected = false,
    )
    KeyMapperTheme {
        KeyMapAppBar(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun HomeStateSelectingDisabledPreview() {
    val state = KeyMapAppBarState.Selecting(
        selectionCount = 4,
        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
        isAllSelected = true,
    )
    KeyMapperTheme {
        KeyMapAppBar(state = state)
    }
}
