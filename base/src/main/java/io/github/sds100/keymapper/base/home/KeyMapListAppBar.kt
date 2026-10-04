package io.github.sds100.keymapper.base.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Keyboard
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.groups.DeleteGroupDialog
import io.github.sds100.keymapper.base.utils.ui.compose.EditableAppBarTextField
import io.github.sds100.keymapper.base.utils.ui.compose.icons.Import
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun KeyMapListAppBar(
    modifier: Modifier = Modifier,
    state: KeyMapAppBarState,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onSortClick: () -> Unit = {},
    onWhatsNewClick: () -> Unit = {},
    onTogglePausedClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onInputMethodPickerClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSelectAllClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onRenameGroupClick: suspend (String) -> Boolean = { true },
    onEditGroupNameClick: () -> Unit = {},
    onDeleteGroupClick: () -> Unit = {},
    onReportBugClick: () -> Unit = {},
    onKeyMapsEnabledChange: (Boolean) -> Unit = {},
) {
    BackHandler(onBack = onBackClick)

    // Use the class as the content key so the content is animated if the data inside the
    // same state class changes.
    when (state) {
        is KeyMapAppBarState.RootGroup -> RootGroupAppBar(
            modifier = modifier,
            state = state,
            scrollBehavior = scrollBehavior,
            onTogglePausedClick = onTogglePausedClick,
            navigationIcon = {
                IconButton(onClick = onSortClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = stringResource(R.string.home_app_bar_sort),
                    )
                }
            },
            actions = {
                var expandedDropdown by rememberSaveable { mutableStateOf(false) }

                AppBarActions(
                    showWhatsNew = true,
                    onWhatsNewClick,
                    onMenuClick = { expandedDropdown = true },
                    dropdownMenuContent = {
                        RootGroupDropdownMenu(
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
                            onInputMethodPickerClick = {
                                expandedDropdown = false
                                onInputMethodPickerClick()
                            },
                            onReportBugClick = {
                                expandedDropdown = false
                                onReportBugClick()
                            },
                            onDismissRequest = { expandedDropdown = false },
                        )
                    },
                )
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
            var newName by remember {
                mutableStateOf(
                    TextFieldValue(
                        state.groupName,
                        selection = TextRange(state.groupName.length),
                    ),
                )
            }
            var showDeleteGroupDialog by remember { mutableStateOf(false) }

            LaunchedEffect(state.groupName) {
                showDeleteGroupDialog = false
                error = null
                val endPosition = state.groupName.length

                if (state.isEditingGroupName) {
                    if (state.isNewGroup) {
                        newName = TextFieldValue()
                    } else {
                        newName =
                            TextFieldValue(state.groupName, selection = TextRange(endPosition))
                    }
                } else {
                    newName =
                        TextFieldValue(state.groupName, selection = TextRange(endPosition))
                }
            }

            if (showDeleteGroupDialog) {
                DeleteGroupDialog(
                    groupName = state.groupName,
                    onDismissRequest = { showDeleteGroupDialog = false },
                    onDeleteClick = onDeleteGroupClick,
                )
            }

            ChildGroupAppBar(
                modifier = modifier,
                groupName = if (state.isEditingGroupName) {
                    newName
                } else {
                    TextFieldValue(state.groupName)
                },
                placeholder = state.groupName,
                error = error,
                onValueChange = {
                    newName = it
                    error = null
                },
                onRenameClick = {
                    scope.launch {
                        if (!onRenameGroupClick(newName.text)) {
                            error = uniqueErrorText
                        }
                    }
                },
                onBackClick = onBackClick,
                onEditClick = onEditGroupNameClick,
                isEditingGroupName = state.isEditingGroupName,
                actions = {
                    AnimatedVisibility(!state.isEditingGroupName) {
                        var expandedDropdown by rememberSaveable { mutableStateOf(false) }

                        Row {
                            KeyMapsEnabledSwitch(
                                state = state.keyMapsEnabled,
                                onChange = onKeyMapsEnabledChange,
                            )

                            AppBarActions(
                                showWhatsNew = false,
                                onWhatsNewClick,
                                onMenuClick = { expandedDropdown = true },
                                dropdownMenuContent = {
                                    ChildGroupDropdownMenu(
                                        expanded = expandedDropdown,
                                        onSortClick = {
                                            expandedDropdown = false
                                            onSortClick()
                                        },
                                        onSettingsClick = {
                                            expandedDropdown = false
                                            onSettingsClick()
                                        },
                                        onAboutClick = {
                                            expandedDropdown = false
                                            onAboutClick()
                                        },
                                        onDismissRequest = { expandedDropdown = false },
                                        onDeleteGroupClick = {
                                            expandedDropdown = false
                                            showDeleteGroupDialog = true
                                        },
                                    )
                                },
                            )
                        }
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
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        title = {
            AppBarStatus(
                isPaused = state.isPaused,
                warnings = state.warnings,
                onTogglePausedClick = onTogglePausedClick,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildGroupAppBar(
    modifier: Modifier = Modifier,
    groupName: TextFieldValue,
    placeholder: String,
    onValueChange: (TextFieldValue) -> Unit = {},
    error: String? = null,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    isEditingGroupName: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // Make custom top app bar because the height can not be set to fix the text field error in.
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            Modifier
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
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

            Spacer(Modifier.width(4.dp))

            EditableAppBarTextField(
                modifier = Modifier.weight(1f),
                value = groupName,
                onValueChange = onValueChange,
                placeholder = placeholder,
                onConfirmClick = onRenameClick,
                error = error,
                isEditing = isEditingGroupName,
                onEditClick = onEditClick,
                confirmContentDescription = stringResource(R.string.home_app_bar_save_group_name),
                editContentDescription = stringResource(R.string.home_app_bar_edit_group_name),
            )

            Spacer(Modifier.width(4.dp))

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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyMapsEnabledSwitch(state: SelectedKeyMapsEnabled?, onChange: (Boolean) -> Unit) {
    val text = when (state) {
        SelectedKeyMapsEnabled.ALL -> stringResource(R.string.home_enabled_key_maps_enabled)
        SelectedKeyMapsEnabled.MIXED -> stringResource(R.string.home_enabled_key_maps_mixed)
        SelectedKeyMapsEnabled.NONE, null -> stringResource(
            R.string.home_enabled_key_maps_disabled,
        )
    }
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below,
        ),
        tooltip = { PlainTooltip { Text(text) } },
        state = tooltipState,
    ) {
        Switch(
            modifier = Modifier.semantics { contentDescription = text },
            checked = state == SelectedKeyMapsEnabled.ALL,
            onCheckedChange = {
                onChange(it)
                scope.launch { tooltipState.show() }
            },
            enabled = state != null,
        )
    }
}

@Composable
private fun AppBarActions(
    showWhatsNew: Boolean,
    onWhatsNewClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    dropdownMenuContent: @Composable () -> Unit,
) {
    Row {
        if (showWhatsNew) {
            IconButton(onClick = onWhatsNewClick) {
                Icon(
                    Icons.Rounded.Campaign,
                    contentDescription = stringResource(R.string.home_app_bar_whats_new),
                )
            }
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.home_app_bar_more),
            )
        }

        dropdownMenuContent()
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
            slideInVertically { height -> -height } + fadeIn() togetherWith
                slideOutVertically { height -> height } + fadeOut()

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

private fun selectedTextTransition(targetState: Int, initialState: Int): ContentTransform {
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
private fun RootGroupDropdownMenu(
    expanded: Boolean,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onInputMethodPickerClick: () -> Unit = {},
    onReportBugClick: () -> Unit = {},
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
            leadingIcon = { Icon(Icons.Rounded.Keyboard, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_input_method_picker)) },
            onClick = onInputMethodPickerClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_about)) },
            onClick = onAboutClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_report_bug)) },
            onClick = onReportBugClick,
        )
    }
}

@Composable
private fun ChildGroupDropdownMenu(
    expanded: Boolean,
    onSortClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onDeleteGroupClick: () -> Unit = {},
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_delete_group)) },
            onClick = onDeleteGroupClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null) },
            text = { Text(stringResource(R.string.home_app_bar_sort)) },
            onClick = onSortClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_settings)) },
            onClick = onSettingsClick,
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            text = { Text(stringResource(R.string.home_menu_about)) },
            onClick = onAboutClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun KeyMapsChildGroupPreview() {
    val state = KeyMapAppBarState.ChildGroup(
        groupName = "Very very very very very long name",
        subGroups = groupSampleList(),
        constraints = constraintsSampleList(),
        parentConstraintCount = 1,
        constraintMode = ConstraintMode.AND,
        breadcrumbs = groupSampleList(),
        isEditingGroupName = false,
        isNewGroup = false,
        keyMapsEnabled = SelectedKeyMapsEnabled.ALL,
    )
    KeyMapperTheme {
        KeyMapListAppBar(modifier = Modifier.fillMaxWidth(), state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun KeyMapsChildGroupDarkPreview() {
    val state = KeyMapAppBarState.ChildGroup(
        groupName = "Short name",
        subGroups = groupSampleList(),
        constraints = emptyList(),
        constraintMode = ConstraintMode.AND,
        parentConstraintCount = 0,
        breadcrumbs = emptyList(),
        isEditingGroupName = false,
        isNewGroup = false,
        keyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
    )
    KeyMapperTheme(darkTheme = true) {
        KeyMapListAppBar(modifier = Modifier.fillMaxWidth(), state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun KeyMapsChildGroupEditingPreview() {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect("") {
        focusRequester.requestFocus()
    }

    KeyMapperTheme {
        ChildGroupAppBar(
            groupName = TextFieldValue(""),
            placeholder = "Untitled group 23",
            error = stringResource(R.string.home_app_bar_group_name_unique_error),
            isEditingGroupName = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun KeyMapsChildGroupEditingDarkPreview() {
    val state = KeyMapAppBarState.ChildGroup(
        groupName = "Untitled group 23",
        subGroups = groupSampleList(),
        constraints = constraintsSampleList(),
        parentConstraintCount = 3,
        constraintMode = ConstraintMode.AND,
        breadcrumbs = emptyList(),
        isEditingGroupName = true,
        isNewGroup = true,
        keyMapsEnabled = SelectedKeyMapsEnabled.ALL,
    )

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect("") {
        focusRequester.requestFocus()
    }

    KeyMapperTheme(darkTheme = true) {
        KeyMapListAppBar(
            state = state,
        )
    }
}

@Preview
@Composable
private fun KeyMapsChildGroupErrorPreview() {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect("") {
        focusRequester.requestFocus()
    }

    KeyMapperTheme {
        ChildGroupAppBar(
            groupName = TextFieldValue("Untitled group 23"),
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
        KeyMapListAppBar(state = state)
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
        KeyMapListAppBar(state = state)
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
        KeyMapListAppBar(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun HomeStateSelectingPreview() {
    val state = KeyMapAppBarState.Selecting(
        selectionCount = 4,
        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
        isAllSelected = false,
        groups = emptyList(),
        breadcrumbs = emptyList(),
        showThisGroup = false,
    )
    KeyMapperTheme {
        KeyMapListAppBar(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStateSelectingDisabledPreview() {
    val state = KeyMapAppBarState.Selecting(
        selectionCount = 4,
        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
        isAllSelected = true,
        groups = emptyList(),
        breadcrumbs = emptyList(),
        showThisGroup = false,
    )
    KeyMapperTheme {
        KeyMapListAppBar(state = state)
    }
}
