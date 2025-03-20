package io.github.sds100.keymapper.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.RestoreType
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.floating.FloatingLayoutsScreen
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListScreen
import io.github.sds100.keymapper.mappings.keymaps.trigger.DpadTriggerSetupBottomSheet
import io.github.sds100.keymapper.sorting.SortBottomSheet
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.ShareUtils
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigateEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    val homeState by viewModel.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBarItems by viewModel.navBarItems.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val setupGuiKeyboardState by viewModel.keymapListViewModel.setupGuiKeyboardState.collectAsStateWithLifecycle()

    if (viewModel.keymapListViewModel.showDpadTriggerSetupBottomSheet) {
        DpadTriggerSetupBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                viewModel.keymapListViewModel.showDpadTriggerSetupBottomSheet =
                    false
            },
            guiKeyboardState = setupGuiKeyboardState,
            onEnableKeyboardClick = viewModel.keymapListViewModel::onEnableGuiKeyboardClick,
            onChooseKeyboardClick = viewModel.keymapListViewModel::onChooseGuiKeyboardClick,
            onNeverShowAgainClick = viewModel.keymapListViewModel::onNeverShowSetupDpadClick,
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

    val importFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            viewModel.onChooseImportFile(uri.toString())
        }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val helpUrl = stringResource(R.string.url_quick_start_guide)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarState = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    val importExportState by viewModel.importExportState.collectAsStateWithLifecycle()
    importExportState.also { exportState ->
        when (exportState) {
            is ImportExportState.Error -> {
                val text = stringResource(R.string.home_export_error_snackbar, exportState.error)
                scope.launch {
                    snackbarState.currentSnackbarData?.dismiss()
                    snackbarState.showSnackbar(text, duration = SnackbarDuration.Short)
                    viewModel.setImportExportIdle()
                }
            }

            ImportExportState.Exporting -> {
                val text = stringResource(R.string.home_exporting_snackbar)
                scope.launch {
                    snackbarState.showSnackbar(text, duration = SnackbarDuration.Indefinite)
                }
            }

            ImportExportState.Importing -> {
                val text = stringResource(R.string.home_importing_snackbar)
                scope.launch {
                    snackbarState.showSnackbar(text, duration = SnackbarDuration.Indefinite)
                }
            }

            is ImportExportState.FinishedExport -> {
                snackbarState.currentSnackbarData?.dismiss()
                ShareUtils.sendZipFile(ctx, exportState.uri.toUri())
                viewModel.setImportExportIdle()
            }

            is ImportExportState.FinishedImport -> {
                val text = stringResource(R.string.home_importing_finished_snackbar)
                scope.launch {
                    snackbarState.currentSnackbarData?.dismiss()
                    snackbarState.showSnackbar(text, duration = SnackbarDuration.Short)
                    viewModel.setImportExportIdle()
                }
            }

            ImportExportState.Idle -> {
                snackbarState.currentSnackbarData?.dismiss()
            }

            is ImportExportState.ConfirmImport -> {
                snackbarState.currentSnackbarData?.dismiss()
                ImportDialog(
                    keyMapCount = exportState.keyMapCount,
                    onDismissRequest = viewModel::setImportExportIdle,
                    onAppendClick = { viewModel.onConfirmImport(RestoreType.APPEND) },
                    onReplaceClick = { viewModel.onConfirmImport(RestoreType.REPLACE) },
                )
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    HomeScreen(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        navController = navController,
        snackBarState = snackbarState,
        navBarItems = navBarItems,
        topAppBar = {
            HomeAppBar(
                scrollBehavior = scrollBehavior,
                homeState = homeState,
                onSettingsClick = onSettingsClick,
                onAboutClick = onAboutClick,
                onSortClick = { viewModel.showSortBottomSheet = true },
                onHelpClick = { uriHandler.openUri(helpUrl) },
                onExportClick = viewModel::onExportClick,
                onImportClick = { importFileLauncher.launch(FileUtils.MIME_TYPE_ALL) },
                onTogglePausedClick = viewModel::onTogglePausedClick,
                onFixWarningClick = viewModel::onFixWarningClick,
            )
        },
        keyMapsContent = {
            KeyMapListScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel.keymapListViewModel,
            )
        },
        floatingButtonsContent = {
            FloatingLayoutsScreen(
                Modifier.fillMaxSize(),
                viewModel = viewModel.listFloatingLayoutsViewModel,
                navController = navController,
            )
        },
        floatingActionButton = {
            val showFab = if (homeState is HomeState.Normal) {
                if (currentDestination?.route == HomeDestination.FloatingButtons.route) {
                    (homeState as HomeState.Normal).showNewLayoutButton
                } else {
                    true
                }
            } else {
                false
            }

            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (currentDestination?.route == HomeDestination.FloatingButtons.route) {
                            viewModel.listFloatingLayoutsViewModel.onNewLayoutClick()
                        } else {
                            scope.launch {
                                viewModel.navigate(
                                    NavigateEvent(
                                        "config_key_map",
                                        NavDestination.ConfigKeyMap(keyMapUid = null),
                                    ),
                                )
                            }
                        }
                    },
                    text = {
                        val fabText = when (currentDestination?.route) {
                            HomeDestination.FloatingButtons.route -> stringResource(R.string.home_fab_new_floating_layout)
                            else -> stringResource(R.string.home_fab_new_key_map)
                        }
                        AnimatedContent(fabText) { text ->
                            Text(text)
                        }
                    },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                )
            }
        },
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    snackBarState: SnackbarHostState = SnackbarHostState(),
    navBarItems: List<HomeNavBarItem>,
    topAppBar: @Composable () -> Unit,
    keyMapsContent: @Composable () -> Unit,
    floatingButtonsContent: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        topBar = topAppBar,
        snackbarHost = {
            SnackbarHost(hostState = snackBarState)
        },
        floatingActionButton = floatingActionButton,
        bottomBar = {
            if (navBarItems.size <= 1) {
                return@Scaffold
            }

            NavigationBar {
                navBarItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            if (item.badge == null) {
                                Icon(item.icon, contentDescription = null)
                            } else {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            modifier = Modifier
                                                .height(22.dp)
                                                .padding(start = 10.dp),
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ) {
                                            Text(
                                                modifier = Modifier.padding(horizontal = 2.dp),
                                                text = item.badge,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(item.icon, contentDescription = null)
                                }
                            }
                        },
                        label = {
                            Text(
                                item.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true,
                        onClick = {
                            // don't do anything if clicking on the current
                            // destination because this results in some ugly animations.
                            if (currentDestination?.route == item.destination.route) {
                                return@NavigationBarItem
                            }

                            navController.navigate(item.destination.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when re-selecting a previously selected item
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = innerPadding.calculateStartPadding(layoutDirection)
        val endPadding = innerPadding.calculateEndPadding(layoutDirection)

        NavHost(
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
                start = startPadding,
                end = endPadding,
            ),
            contentAlignment = Alignment.TopCenter,
            navController = navController,
            startDestination = HomeDestination.KeyMaps.route,
            // use no animations because otherwise the transition freezes
            // when quickly navigating to another page while the transition is still happening.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            composable(HomeDestination.KeyMaps.route) {
                keyMapsContent()
            }
            composable(HomeDestination.FloatingButtons.route) {
                floatingButtonsContent()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeAppBar(
    homeState: HomeState,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onSortClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onTogglePausedClick: () -> Unit = {},
    onFixWarningClick: (String) -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
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
    val appBarColors = TopAppBarDefaults.topAppBarColors()

    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            appBarColors.containerColor,
            appBarColors.scrolledContainerColor,
            FastOutLinearInEasing.transform(colorTransitionFraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    var expandedDropdown by rememberSaveable { mutableStateOf(false) }

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                AnimatedContent(
                    homeState,
                    transitionSpec = {
                        selectedTextTransition(targetState, initialState)
                    },
                ) { homeState ->
                    when (homeState) {
                        is HomeState.Normal -> {
                            AppBarStatus(
                                homeState = homeState,
                                onTogglePausedClick = onTogglePausedClick,
                            )
                        }

                        is HomeState.Selecting -> {
                            Text(stringResource(R.string.selection_count, homeState.selectionCount))
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onSortClick) {
                    AnimatedContent(homeState is HomeState.Selecting) { isSelecting ->
                        if (isSelecting) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.home_app_bar_cancel_selecting),
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = stringResource(R.string.home_app_bar_sort),
                            )
                        }
                    }
                }
            },
            actions = {
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

                HomeDropdownMenu(
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
            },
            colors = appBarColors,
        )
        if (homeState is HomeState.Normal) {
            // TODO handle the case that no app can be found for picking a file
            AnimatedVisibility(homeState.warnings.isNotEmpty()) {
                Surface(color = appBarContainerColor) {
                    WarningList(
                        modifier = Modifier.padding(bottom = 8.dp),
                        warnings = homeState.warnings,
                        onFixClick = onFixWarningClick,
                    )
                }
            }
        }
    }
}

private fun selectedTextTransition(
    targetState: HomeState,
    initialState: HomeState,
): ContentTransform {
    val prevCount = when (initialState) {
        is HomeState.Normal -> 0
        is HomeState.Selecting -> initialState.selectionCount
    }

    val count = when (targetState) {
        is HomeState.Normal -> 0
        is HomeState.Selecting -> targetState.selectionCount
    }

    return slideInVertically { height ->
        if (count > prevCount) {
            -height
        } else {
            height
        }
    } + fadeIn() togetherWith slideOutVertically { height ->
        if (count > prevCount) {
            height
        } else {
            -height
        }
    } + fadeOut()
}

@Composable
private fun HomeDropdownMenu(
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
            leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
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
private fun AppBarStatus(
    homeState: HomeState.Normal,
    onTogglePausedClick: () -> Unit,
) {
    val pausedButtonContainerColor by animateColorAsState(
        targetValue = if (homeState.isPaused || homeState.warnings.isNotEmpty()) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            LocalCustomColorsPalette.current.greenContainer
        },
    )

    val pausedButtonContentColor by animateColorAsState(
        targetValue = if (homeState.isPaused || homeState.warnings.isNotEmpty()) {
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

        if (homeState.isPaused) {
            buttonIcon = Icons.Rounded.PauseCircleOutline
            buttonText = stringResource(R.string.home_app_bar_status_paused)
        } else if (homeState.warnings.isNotEmpty()) {
            buttonIcon = Icons.Rounded.ErrorOutline
            buttonText = pluralStringResource(
                R.plurals.home_app_bar_status_warnings,
                homeState.warnings.size,
                homeState.warnings.size,
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
private fun WarningList(
    modifier: Modifier = Modifier,
    warnings: List<HomeWarningListItem>,
    onFixClick: (String) -> Unit,
) {
    OutlinedCard(
        modifier = modifier.padding(horizontal = 8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 5.dp),
    ) {
        Column(
            Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (warning in warnings) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        modifier = Modifier.weight(1f),
                        text = warning.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = { onFixClick(warning.id) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(stringResource(R.string.button_fix))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportDialog(
    modifier: Modifier = Modifier,
    keyMapCount: Int,
    onDismissRequest: () -> Unit,
    onAppendClick: () -> Unit,
    onReplaceClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                pluralStringResource(
                    R.plurals.home_importing_dialog_title,
                    keyMapCount,
                    keyMapCount,
                ),
            )
        },
        text = {
            Text(
                stringResource(R.string.home_importing_dialog_text, keyMapCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onAppendClick) {
                Text(stringResource(R.string.home_importing_dialog_append))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.home_importing_dialog_cancel))
            }

            TextButton(onClick = onReplaceClick) {
                Text(stringResource(R.string.home_importing_dialog_replace))
            }
        },
    )
}

private fun sampleNavBarItems(): List<HomeNavBarItem> {
    return listOf(
        HomeNavBarItem(
            icon = Icons.Outlined.Gamepad,
            label = "Key Maps",
            destination = HomeDestination.KeyMaps,
        ),
        HomeNavBarItem(
            icon = Icons.Outlined.BubbleChart,
            label = "Floating Buttons",
            destination = HomeDestination.FloatingButtons,
            badge = "NEW!",
        ),
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeStateRunningPreview() {
    val state = HomeState.Normal(warnings = emptyList(), isPaused = false)
    KeyMapperTheme {
        HomeScreen(
            navController = rememberNavController(),
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
private fun HomeStateSelectingPreview() {
    val state = HomeState.Selecting(
        selectionCount = 4,
        selectedKeyMapsEnabled = true,
    )
    KeyMapperTheme {
        HomeScreen(
            navController = rememberNavController(),
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar(state) },
            keyMapsContent = {},
            floatingButtonsContent = {},
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
