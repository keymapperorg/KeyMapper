package io.github.sds100.keymapper.home

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.keymaps.trigger.DpadTriggerSetupBottomSheet
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigateEvent
import kotlinx.coroutines.launch

private data class HomeNavBarItem(
    val destination: HomeDestination,
    val label: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.keymapListViewModel.setupGuiKeyboardState.collectAsStateWithLifecycle()

    if (viewModel.keymapListViewModel.showDpadTriggerSetupBottomSheet) {
        DpadTriggerSetupBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                viewModel.keymapListViewModel.showDpadTriggerSetupBottomSheet =
                    false
            },
            guiKeyboardState = state,
            onEnableKeyboardClick = viewModel.keymapListViewModel::onEnableGuiKeyboardClick,
            onChooseKeyboardClick = viewModel.keymapListViewModel::onChooseGuiKeyboardClick,
            onNeverShowAgainClick = viewModel.keymapListViewModel::onNeverShowSetupDpadClick,
            sheetState = sheetState,
        )
    }

    val scope = rememberCoroutineScope()

    HomeScreen(
        modifier = Modifier
            .systemBarsPadding()
            .displayCutoutPadding(),
        onMenuClick = {
            scope.launch {
                viewModel.navigate(NavigateEvent("settings", NavDestination.Settings))
            }
        },
        keyMapsContent = {
            Text("Key maps")

            LazyColumn(modifier = Modifier.fillMaxSize()) { }

            FloatingActionButton(onClick = {
                scope.launch {
                    viewModel.navigate(
                        NavigateEvent(
                            "config_key_map",
                            NavDestination.ConfigKeyMap(keyMapUid = null),
                        ),
                    )
                }
            }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
            }
        },
        floatingButtonsContent = {
            Text("Floating buttons")
        },
        fingerprintMapsContent = {
            Text("Fingerprint maps")
        },
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    keyMapsContent: @Composable () -> Unit,
    floatingButtonsContent: @Composable () -> Unit,
    fingerprintMapsContent: @Composable () -> Unit,
) {
    val navController = rememberNavController()

    val navBarItems = listOf(
        HomeNavBarItem(
            HomeDestination.KeyMaps,
            label = stringResource(R.string.home_nav_bar_key_maps),
            icon = Icons.Outlined.Gamepad,
        ),
        HomeNavBarItem(
            HomeDestination.FloatingButtons,
            label = stringResource(R.string.home_nav_bar_floating_buttons),
            icon = Icons.Outlined.BubbleChart,
        ),
        HomeNavBarItem(
            HomeDestination.FingerprintMaps,
            label = stringResource(R.string.home_nav_bar_fingerprint_maps),
            icon = Icons.Outlined.Fingerprint,
        ),
    )
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = { HomeAppBar(onMenuClick = onMenuClick) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

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
            composable(HomeDestination.FingerprintMaps.route) {
                fingerprintMapsContent()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeAppBar(onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text("Running")
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = null)
            }
        },
        actions = {
        },
        colors = TopAppBarDefaults.topAppBarColors()
            .copy(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Preview
@Composable
private fun NavigationPreview() {
    HomeScreen(keyMapsContent = {}, floatingButtonsContent = {}, fingerprintMapsContent = {})
}
