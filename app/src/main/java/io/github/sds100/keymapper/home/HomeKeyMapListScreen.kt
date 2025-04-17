package io.github.sds100.keymapper.home

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.ImportExportState
import io.github.sds100.keymapper.backup.RestoreType
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.groups.GroupListItemModel
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAppBarState
import io.github.sds100.keymapper.mappings.keymaps.KeyMapList
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListViewModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.DpadTriggerSetupBottomSheet
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.sorting.SortBottomSheet
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.ShareUtils
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.CollapsableFloatingActionButton
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.compose.openUriSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeKeyMapListScreen(
    modifier: Modifier = Modifier,
    viewModel: KeyMapListViewModel,
    snackbarState: SnackbarHostState,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    finishActivity: () -> Unit,
    fabBottomPadding: Dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val setupGuiKeyboardState by viewModel.setupGuiKeyboardState.collectAsStateWithLifecycle()

    val importFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            viewModel.onChooseImportFile(uri.toString())
        }

    val importExportState by viewModel.importExportState.collectAsStateWithLifecycle()

    HandleImportExportState(
        state = importExportState,
        snackbarState = snackbarState,
        setIdleState = viewModel::setImportExportIdle,
        onConfirmImport = viewModel::onConfirmImport,
    )

    if (viewModel.showDpadTriggerSetupBottomSheet) {
        DpadTriggerSetupBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                viewModel.showDpadTriggerSetupBottomSheet = false
            },
            guiKeyboardState = setupGuiKeyboardState,
            onEnableKeyboardClick = viewModel::onEnableGuiKeyboardClick,
            onChooseKeyboardClick = viewModel::onChooseGuiKeyboardClick,
            onNeverShowAgainClick = viewModel::onNeverShowSetupDpadClick,
            sheetState = sheetState,
        )
    }

    if (viewModel.showSortBottomSheet) {
        SortBottomSheet(
            viewModel = viewModel.sortViewModel,
            onDismissRequest = { viewModel.showSortBottomSheet = false },
            sheetState = sheetState,
        )
    }

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog && state.appBarState is KeyMapAppBarState.Selecting) {
        val keyMapCount = (state.appBarState as KeyMapAppBarState.Selecting).selectionCount

        DeleteKeyMapsDialog(
            keyMapCount = keyMapCount,
            onDismissRequest = { showDeleteDialog = false },
            onDeleteClick = {
                viewModel.onDeleteSelectedKeyMapsClick()
                showDeleteDialog = false
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    val ctx = LocalContext.current
    val helpUrl = stringResource(R.string.url_quick_start_guide)

    var keyMapListBottomPadding by remember { mutableStateOf(100.dp) }

    HomeKeyMapListScreen(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarState = snackbarState,
        floatingActionButton = {
            AnimatedVisibility(
                state.appBarState !is KeyMapAppBarState.Selecting,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            ) {
                CollapsableFloatingActionButton(
                    modifier = Modifier.padding(bottom = fabBottomPadding),
                    onClick = viewModel::onNewKeyMapClick,
                    showText = viewModel.showFabText,
                    text = stringResource(R.string.home_fab_new_key_map),
                )
            }
        },
        listContent = {
            KeyMapList(
                modifier = Modifier.animateContentSize(),
                lazyListState = rememberLazyListState(),
                listItems = state.listItems,
                footerText = stringResource(R.string.home_key_map_list_footer_text),
                isSelectable = state.appBarState is KeyMapAppBarState.Selecting,
                onClickKeyMap = viewModel::onKeyMapCardClick,
                onLongClickKeyMap = viewModel::onKeyMapCardLongClick,
                onSelectedChange = viewModel::onKeyMapSelectedChanged,
                onFixClick = viewModel::onFixClick,
                onTriggerErrorClick = viewModel::onFixTriggerError,
                bottomListPadding = keyMapListBottomPadding,
            )
        },
        appBarContent = {
            KeyMapListAppBar(
                state = state.appBarState,
                scrollBehavior = scrollBehavior,
                onSettingsClick = onSettingsClick,
                onAboutClick = onAboutClick,
                onSortClick = { viewModel.showSortBottomSheet = true },
                onHelpClick = { uriHandler.openUriSafe(ctx, helpUrl) },
                onExportClick = viewModel::onExportClick,
                onImportClick = { importFileLauncher.launch(FileUtils.MIME_TYPE_ALL) },
                onTogglePausedClick = viewModel::onTogglePausedClick,
                onFixWarningClick = viewModel::onFixWarningClick,
                onBackClick = {
                    if (!viewModel.onBackClick()) {
                        finishActivity()
                    }
                },
                onSelectAllClick = viewModel::onSelectAllClick,
                onNewGroupClick = viewModel::onNewGroupClick,
                onRenameGroupClick = viewModel::onRenameGroupClick,
                onEditGroupNameClick = viewModel::onEditGroupNameClick,
                onGroupClick = viewModel::onGroupClick,
                onDeleteGroupClick = viewModel::onDeleteGroupClick,
                onNewConstraintClick = viewModel::onNewGroupConstraintClick,
                onRemoveConstraintClick = viewModel::onRemoveGroupConstraintClick,
                onConstraintModeChanged = viewModel::onGroupConstraintModeChanged,
                onFixConstraintClick = viewModel::onFixClick,
            )
        },
        selectionBottomSheet = {
            AnimatedVisibility(
                visible = state.appBarState is KeyMapAppBarState.Selecting,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                val selectionState = (state.appBarState as? KeyMapAppBarState.Selecting)
                    ?: KeyMapAppBarState.Selecting(
                        selectionCount = 0,
                        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.NONE,
                        isAllSelected = false,
                        groups = emptyList(),
                        breadcrumbs = emptyList(),
                        showThisGroup = false,
                    )

                SelectionBottomSheet(
                    modifier = Modifier.onSizeChanged { size ->
                        keyMapListBottomPadding =
                            ((size.height.dp / 2) - 100.dp).coerceAtLeast(0.dp)
                    },
                    enabled = selectionState.selectionCount > 0,
                    groups = selectionState.groups,
                    breadcrumbs = selectionState.breadcrumbs,
                    selectedKeyMapsEnabled = selectionState.selectedKeyMapsEnabled,
                    onEnabledKeyMapsChange = viewModel::onEnabledKeyMapsChange,
                    onDuplicateClick = viewModel::onDuplicateSelectedKeyMapsClick,
                    onExportClick = viewModel::onExportSelectedKeyMaps,
                    onDeleteClick = { showDeleteDialog = true },
                    onGroupClick = viewModel::onSelectionGroupClick,
                    onNewGroupClick = viewModel::onNewGroupClick,
                    showThisGroup = selectionState.showThisGroup,
                    onThisGroupClick = viewModel::onMoveToThisGroupClick,
                )
            }
        },
    )
}

@Composable
private fun HomeKeyMapListScreen(
    modifier: Modifier = Modifier,
    snackbarState: SnackbarHostState = SnackbarHostState(),
    appBarContent: @Composable () -> Unit,
    listContent: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    selectionBottomSheet: @Composable () -> Unit,
) {
    Scaffold(
        modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarState) },
        topBar = appBarContent,
        floatingActionButton = floatingActionButton,
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                listContent()
                selectionBottomSheet()
            }
        }
    }
}

@Composable
fun HandleImportExportState(
    state: ImportExportState,
    snackbarState: SnackbarHostState,
    setIdleState: () -> Unit,
    onConfirmImport: (RestoreType) -> Unit,
) {
    when (state) {
        is ImportExportState.Error -> {
            val text = stringResource(R.string.home_export_error_snackbar, state.error)
            LaunchedEffect(state) {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(text, duration = SnackbarDuration.Short)
                setIdleState()
            }
        }

        ImportExportState.Exporting -> {
            val text = stringResource(R.string.home_exporting_snackbar)
            LaunchedEffect(state) {
                snackbarState.showSnackbar(text, duration = SnackbarDuration.Indefinite)
            }
        }

        ImportExportState.Importing -> {
            val text = stringResource(R.string.home_importing_snackbar)
            LaunchedEffect(state) {
                snackbarState.showSnackbar(text, duration = SnackbarDuration.Indefinite)
            }
        }

        is ImportExportState.FinishedExport -> {
            snackbarState.currentSnackbarData?.dismiss()
            LocalActivity.current?.let { ShareUtils.shareFile(it, state.uri.toUri()) }
            setIdleState()
        }

        is ImportExportState.FinishedImport -> {
            val text = stringResource(R.string.home_importing_finished_snackbar)
            LaunchedEffect(state) {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(text, duration = SnackbarDuration.Short)
                setIdleState()
            }
        }

        ImportExportState.Idle -> {
            snackbarState.currentSnackbarData?.dismiss()
        }

        is ImportExportState.ConfirmImport -> {
            snackbarState.currentSnackbarData?.dismiss()
            ImportDialog(
                keyMapCount = state.keyMapCount,
                onDismissRequest = setIdleState,
                onAppendClick = { onConfirmImport(RestoreType.APPEND) },
                onReplaceClick = { onConfirmImport(RestoreType.REPLACE) },
            )
        }
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
                triggerKeys = listOf("Volume down", "Volume up", "Volume down"),
                triggerSeparatorIcon = Icons.AutoMirrored.Outlined.ArrowForward,
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
                extraInfo = "Disabled • No trigger",
            ),
        ),
        KeyMapListItemModel(
            isSelected = true,
            KeyMapListItemModel.Content(
                uid = "1",
                triggerKeys = listOf("Volume down", "Volume up"),
                triggerSeparatorIcon = Icons.Outlined.Add,
                actions = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Open Key Mapper",
                    ),
                ),
                constraintMode = ConstraintMode.AND,
                constraints = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Key Mapper is not open",
                    ),
                ),
                options = listOf(
                    "Vibrate",
                    "Vibrate when keys are initially pressed and again when long pressed",
                ),
                triggerErrors = emptyList(),
                extraInfo = null,
            ),
        ),
        KeyMapListItemModel(
            isSelected = true,
            KeyMapListItemModel.Content(
                uid = "2",
                triggerKeys = listOf("Volume down", "Volume up"),
                triggerSeparatorIcon = Icons.Outlined.Add,
                actions = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Open Key Mapper",
                    ),
                ),
                constraintMode = ConstraintMode.AND,
                constraints = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Key Mapper is not open",
                    ),
                ),
                options = emptyList(),
                triggerErrors = emptyList(),
                extraInfo = null,
            ),
        ),
        KeyMapListItemModel(
            isSelected = true,
            KeyMapListItemModel.Content(
                uid = "3",
                triggerKeys = listOf("Volume down", "Volume up"),
                triggerSeparatorIcon = Icons.Outlined.Add,
                actions = listOf(
                    ComposeChipModel.Normal(
                        id = "0",
                        ComposeIconInfo.Drawable(drawable = context.drawable(R.drawable.ic_launcher_web)),
                        "Open Key Mapper",
                    ),
                ),
                constraintMode = ConstraintMode.AND,
                constraints = emptyList(),
                options = emptyList(),
                triggerErrors = emptyList(),
                extraInfo = null,
            ),
        ),
        KeyMapListItemModel(
            isSelected = false,
            content = KeyMapListItemModel.Content(
                uid = "4",
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewSelectingKeyMaps() {
    val appBarState = KeyMapAppBarState.Selecting(
        selectionCount = 2,
        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
        isAllSelected = false,
        groups = emptyList(),
        breadcrumbs = emptyList(),
        showThisGroup = false,
    )

    val listState = State.Data(sampleList())

    KeyMapperTheme {
        HomeKeyMapListScreen(
            floatingActionButton = {},
            listContent = {
                KeyMapList(
                    lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = 4),
                    listItems = listState,
                    footerText = stringResource(R.string.home_key_map_list_footer_text),
                    isSelectable = true,
                )
            },
            appBarContent = {
                KeyMapListAppBar(state = appBarState)
            },
            selectionBottomSheet = {
                SelectionBottomSheet(
                    enabled = true,
                    selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
                    groups = emptyList(),
                    breadcrumbs = emptyList(),
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewKeyMapsRunning() {
    val appBarState = KeyMapAppBarState.RootGroup(
        subGroups = emptyList(),
        warnings = emptyList(),
        isPaused = false,
    )

    val listState = State.Data(sampleList())

    KeyMapperTheme {
        HomeKeyMapListScreen(
            floatingActionButton = {
                CollapsableFloatingActionButton(
                    showText = true,
                    text = stringResource(R.string.home_fab_new_key_map),
                )
            },
            listContent = {
                KeyMapList(
                    lazyListState = rememberLazyListState(),
                    listItems = listState,
                    footerText = stringResource(R.string.home_key_map_list_footer_text),
                    isSelectable = false,
                )
            },
            appBarContent = {
                KeyMapListAppBar(state = appBarState)
            },
            selectionBottomSheet = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewKeyMapsPaused() {
    val appBarState = KeyMapAppBarState.RootGroup(
        subGroups = emptyList(),
        warnings = emptyList(),
        isPaused = true,
    )

    val listState = State.Data(sampleList())

    KeyMapperTheme {
        HomeKeyMapListScreen(
            floatingActionButton = {
                CollapsableFloatingActionButton(
                    showText = true,
                    text = stringResource(R.string.home_fab_new_key_map),
                )
            },
            listContent = {
                KeyMapList(
                    lazyListState = rememberLazyListState(),
                    listItems = listState,
                    footerText = stringResource(R.string.home_key_map_list_footer_text),
                    isSelectable = false,
                )
            },
            appBarContent = {
                KeyMapListAppBar(state = appBarState)
            },
            selectionBottomSheet = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewKeyMapsWarnings() {
    val ctx = LocalContext.current

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

    val appBarState = KeyMapAppBarState.RootGroup(
        subGroups = listOf(
            GroupListItemModel(
                uid = "0",
                name = "Key Mapper",
                icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
            ),
        ),
        warnings = warnings,
        isPaused = true,
    )

    val listState = State.Data(sampleList())

    KeyMapperTheme {
        HomeKeyMapListScreen(
            floatingActionButton = {
                CollapsableFloatingActionButton(
                    showText = true,
                    text = stringResource(R.string.home_fab_new_key_map),
                )
            },
            listContent = {
                KeyMapList(
                    lazyListState = rememberLazyListState(),
                    listItems = listState,
                    footerText = stringResource(R.string.home_key_map_list_footer_text),
                    isSelectable = false,
                )
            },
            appBarContent = {
                KeyMapListAppBar(state = appBarState)
            },
            selectionBottomSheet = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(device = Devices.PIXEL)
@Composable
private fun PreviewKeyMapsWarningsEmpty() {
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

    val appBarState = KeyMapAppBarState.RootGroup(
        subGroups = emptyList(),
        warnings = warnings,
        isPaused = true,
    )

    val listState = State.Data(emptyList<KeyMapListItemModel>())

    KeyMapperTheme {
        HomeKeyMapListScreen(
            floatingActionButton = {
                CollapsableFloatingActionButton(
                    showText = true,
                    text = stringResource(R.string.home_fab_new_key_map),
                )
            },
            listContent = {
                KeyMapList(
                    lazyListState = rememberLazyListState(),
                    listItems = listState,
                    footerText = stringResource(R.string.home_key_map_list_footer_text),
                    isSelectable = false,
                )
            },
            appBarContent = {
                KeyMapListAppBar(state = appBarState)
            },
            selectionBottomSheet = {},
        )
    }
}
