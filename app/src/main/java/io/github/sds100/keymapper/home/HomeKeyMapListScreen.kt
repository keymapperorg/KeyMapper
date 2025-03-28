package io.github.sds100.keymapper.home

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.ImportExportState
import io.github.sds100.keymapper.backup.RestoreType
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAppBarState
import io.github.sds100.keymapper.mappings.keymaps.KeyMapList
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListViewModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.DpadTriggerSetupBottomSheet
import io.github.sds100.keymapper.sorting.SortBottomSheet
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.ShareUtils
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigateEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeKeyMapListScreen(
    modifier: Modifier = Modifier,
    viewModel: KeyMapListViewModel,
    snackbarState: SnackbarHostState,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    finishActivity: () -> Unit,
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

    if (showDeleteDialog) {
        val keyMapCount = (state.appBarState as? KeyMapAppBarState.Selecting)?.selectionCount ?: 0

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
    val helpUrl = stringResource(R.string.url_quick_start_guide)
    val scope = rememberCoroutineScope()

    HomeKeyMapListScreen(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarState = snackbarState,
        floatingActionButton = {
            if (state.appBarState !is KeyMapAppBarState.Selecting) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            viewModel.navigate(
                                NavigateEvent(
                                    "config_key_map",
                                    NavDestination.ConfigKeyMap(keyMapUid = null),
                                ),
                            )
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fabText = stringResource(R.string.home_fab_new_key_map)
                        Icon(Icons.Rounded.Add, contentDescription = fabText)

                        AnimatedVisibility(viewModel.showFabText) {
                            AnimatedContent(fabText) { text ->
                                Text(modifier = Modifier.padding(start = 8.dp), text = fabText)
                            }
                        }
                    }
                }
            }
        },
        listContent = {
            KeyMapList(
                modifier = modifier,
                lazyListState = rememberLazyListState(),
                listItems = state.listItems,
                footerText = stringResource(R.string.home_key_map_list_footer_text),
                isSelectable = state.appBarState is KeyMapAppBarState.Selecting,
                onClickKeyMap = viewModel::onKeyMapCardClick,
                onLongClickKeyMap = viewModel::onKeyMapCardLongClick,
                onSelectedChange = viewModel::onKeyMapSelectedChanged,
                onFixClick = viewModel::onFixClick,
                onTriggerErrorClick = viewModel::onFixTriggerError,
            )
        },
        appBarContent = {
            KeyMapAppBar(
                state = state.appBarState,
                scrollBehavior = scrollBehavior,
                onSettingsClick = onSettingsClick,
                onAboutClick = onAboutClick,
                onSortClick = { viewModel.showSortBottomSheet = true },
                onHelpClick = { uriHandler.openUri(helpUrl) },
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
            )
        },
    )
}

@Composable
private fun HomeKeyMapListScreen(
    modifier: Modifier = Modifier,
    snackbarState: SnackbarHostState,
    appBarContent: @Composable () -> Unit,
    listContent: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
) {
    Scaffold(
        modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarState) },
        topBar = appBarContent,
        floatingActionButton = floatingActionButton,
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            listContent()
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStateRunningPreview() {
    val state = HomeState.Normal(warnings = emptyList(), isPaused = false)
    KeyMapperTheme {
        HomeScreen(
            navController = rememberNavController(),
            homeState = state,
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar(state) },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStatePausedPreview() {
    val state = HomeState.Normal(warnings = emptyList(), isPaused = true)
    KeyMapperTheme {
        HomeScreen(
            navController = rememberNavController(),
            homeState = state,
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar(state) },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStateWarningsPreview() {
    val state = HomeState.Normal(
        warnings = listOf(
            HomeWarningListItem(
                id = "0",
                text = stringResource(R.string.home_error_accessibility_service_is_disabled),
            ),
            HomeWarningListItem(
                id = "1",
                text = stringResource(R.string.home_error_is_battery_optimised),
            ),
        ),
        isPaused = false,
    )
    KeyMapperTheme {
        HomeScreen(
            navController = rememberNavController(),
            homeState = state,
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar(state) },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStateWarningsDarkPreview() {
    val state = HomeState.Normal(
        warnings = listOf(
            HomeWarningListItem(
                id = "0",
                text = stringResource(R.string.home_error_accessibility_service_is_disabled),
            ),
            HomeWarningListItem(
                id = "1",
                text = stringResource(R.string.home_error_is_battery_optimised),
            ),
        ),
        isPaused = false,
    )
    KeyMapperTheme(darkTheme = true) {
        HomeScreen(
            navController = rememberNavController(),
            homeState = state,
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar(state) },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(widthDp = 300, heightDp = 600)
@Composable
private fun HomeStateSelectingPreview() {
    val state = HomeState.Selecting(
        selectionCount = 4,
        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
        isAllSelected = false,
    )
    KeyMapperTheme {
        HomeScreen(
            navController = rememberNavController(),
            homeState = state,
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar(state) },
            keyMapsContent = {},
            floatingButtonsContent = {},
            selectionBottomSheet = {
                SelectionBottomSheet(
                    enabled = true,
                    selectedKeyMapsEnabled = SelectedKeyMapsEnabled.ALL,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun HomeStateSelectingDisabledPreview() {
    val state = HomeState.Selecting(
        selectionCount = 4,
        selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
        isAllSelected = true,
    )
    KeyMapperTheme {
        HomeScreen(
            navController = rememberNavController(),
            homeState = state,
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar(state) },
            keyMapsContent = {},
            floatingButtonsContent = {},
            selectionBottomSheet = {
                SelectionBottomSheet(
                    enabled = false,
                    selectedKeyMapsEnabled = SelectedKeyMapsEnabled.NONE,
                )
            },
        )
    }
}

@Preview
@Composable
private fun ImportDialogPreview() {
    KeyMapperTheme {
        ImportDialog(
            keyMapCount = 3,
            onDismissRequest = {},
            onAppendClick = {},
            onReplaceClick = {},
        )
    }
}

@Preview
@Composable
private fun DropdownPreview() {
    KeyMapperTheme {
        HomeDropdownMenu(
            expanded = true,
        )
    }
}

@Preview
@Composable
private fun DropdownExportingPreview() {
    KeyMapperTheme {
        HomeDropdownMenu(
            expanded = true,
        )
    }
}
