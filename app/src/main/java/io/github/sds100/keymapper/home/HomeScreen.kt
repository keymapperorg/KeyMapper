package io.github.sds100.keymapper.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.sds100.keymapper.util.ui.SelectionState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    finishActivity: () -> Unit,
    startDestination: HomeDestination = HomeDestination.KeyMaps,
) {
    val navController = rememberNavController()
    val navBarItems by viewModel.navBarItems.collectAsStateWithLifecycle()

    val snackbarState = remember { SnackbarHostState() }
    val selectionState by viewModel.keyMapListViewModel.multiSelectProvider.state.collectAsStateWithLifecycle()

    HomeScreen(
        modifier = modifier,
        isSelectingKeyMaps = selectionState is SelectionState.Selecting,
        startDestination = startDestination,
        navController = navController,
        navBarItems = navBarItems,
        keyMapsContent = {
            HomeKeyMapListScreen(
                viewModel = viewModel.keyMapListViewModel,
                snackbarState = snackbarState,
                onSettingsClick = onSettingsClick,
                onAboutClick = onAboutClick,
                finishActivity = finishActivity,
                fabBottomPadding = if (navBarItems.size == 1) {
                    0.dp
                } else {
                    80.dp
                },
            )
        },
        floatingButtonsContent = {
            HomeFloatingLayoutsScreen(
                viewModel = viewModel.listFloatingLayoutsViewModel,
                navController = navController,
                snackbarState = snackbarState,
                fabBottomPadding = if (navBarItems.size == 1) {
                    0.dp
                } else {
                    80.dp
                },
            )
        },
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    isSelectingKeyMaps: Boolean,
    startDestination: HomeDestination = HomeDestination.KeyMaps,
    navController: NavHostController,
    navBarItems: List<HomeNavBarItem>,
    keyMapsContent: @Composable () -> Unit,
    floatingButtonsContent: @Composable () -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Column(modifier) {
        Box(contentAlignment = Alignment.BottomCenter) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
                navController = navController,
                startDestination = startDestination.route,
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

            this@Column.AnimatedVisibility(
                visible = !isSelectingKeyMaps && navBarItems.size > 1,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
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
            }
        }
    }
}
