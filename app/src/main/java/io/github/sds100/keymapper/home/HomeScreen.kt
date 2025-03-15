package io.github.sds100.keymapper.home

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.floating.FloatingLayoutsScreen
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListScreen
import io.github.sds100.keymapper.mappings.keymaps.trigger.DpadTriggerSetupBottomSheet
import io.github.sds100.keymapper.sorting.SortBottomSheet
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigateEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val homeState by viewModel.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBarItems by viewModel.navBarItems.collectAsStateWithLifecycle()

    val showNewLayoutFab by viewModel.listFloatingLayoutsViewModel.showNewLayoutFab
        .collectAsStateWithLifecycle(false)

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

    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    if (showSortBottomSheet) {
        SortBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            viewModel = viewModel.sortViewModel,
        )
    }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val helpUrl = stringResource(R.string.url_quick_start_guide)

    HomeScreen(
        state = homeState,
        navController = navController,
        navBarItems = navBarItems,
        topAppBar = {
            HomeAppBar(
                onMenuClick = {},
                onSortClick = { showSortBottomSheet = true },
                onHelpClick = {
                    uriHandler.openUri(helpUrl)
                },
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
        floatingActionButton = { destination ->
            when (destination) {
                HomeDestination.KeyMaps.route -> {
                    ExtendedFloatingActionButton(
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
                        text = { Text(stringResource(R.string.home_fab_new_key_map)) },
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    )
                }

                HomeDestination.FloatingButtons.route -> {
                    if (showNewLayoutFab) {
                        ExtendedFloatingActionButton(
                            onClick = viewModel.listFloatingLayoutsViewModel::onNewLayoutClick,
                            text = { Text(stringResource(R.string.home_fab_new_floating_layout)) },
                            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeState,
    navController: NavHostController,
    navBarItems: List<HomeNavBarItem>,
    topAppBar: @Composable () -> Unit,
    keyMapsContent: @Composable () -> Unit,
    floatingButtonsContent: @Composable () -> Unit,
    floatingActionButton: @Composable (destination: String?) -> Unit = {},
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = topAppBar,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = { floatingActionButton(currentDestination?.route) },
        bottomBar = {
            if (navBarItems.size <= 1) {
                return@Scaffold
            }

            NavigationBar {
                navBarItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
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
    onMenuClick: () -> Unit = {},
    onSortClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text("Running")
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = stringResource(R.string.home_app_bar_menu),
                )
            }
        },
        actions = {
            IconButton(onClick = onSortClick) {
                Icon(
                    Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = stringResource(R.string.home_app_bar_sort),
                )
            }
            IconButton(onClick = onHelpClick) {
                Icon(
                    Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = stringResource(R.string.home_app_bar_help),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
            .copy(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
        ),
    )
}

@Preview
@Composable
private fun HomeStateRunningPreview() {
    KeyMapperTheme {
        HomeScreen(
            state = HomeState.Normal(warnings = emptyList(), isPaused = false),
            navController = rememberNavController(),
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar() },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}

@Preview
@Composable
private fun HomeStatePausedPreview() {
    KeyMapperTheme {
        HomeScreen(
            state = HomeState.Normal(warnings = emptyList(), isPaused = true),
            navController = rememberNavController(),
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar() },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}

@Preview
@Composable
private fun HomeStateWarningsPreview() {
    KeyMapperTheme {
        HomeScreen(
            state = HomeState.Normal(
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
                isPaused = true,
            ),
            navController = rememberNavController(),
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar() },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}

@Preview
@Composable
private fun HomeStateSelectingPreview() {
    KeyMapperTheme {
        HomeScreen(
            state = HomeState.Selecting(
                selectionCount = 4,
                allKeyMapsEnabled = true,
            ),
            navController = rememberNavController(),
            navBarItems = sampleNavBarItems(),
            topAppBar = { HomeAppBar() },
            keyMapsContent = {},
            floatingButtonsContent = {},
        )
    }
}
