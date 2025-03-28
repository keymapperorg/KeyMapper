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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Sort
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAppBarState
import io.github.sds100.keymapper.util.ui.compose.icons.Import
import io.github.sds100.keymapper.util.ui.compose.icons.KeyMapperIcons

@Composable
@OptIn(ExperimentalMaterial3Api::class)
 fun KeyMapAppBar(
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

    val appBarColors = if (state is KeyMapAppBarState.Selecting) {
        TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        TopAppBarDefaults.centerAlignedTopAppBarColors()
    }

    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            appBarColors.containerColor,
            appBarColors.scrolledContainerColor,
            FastOutLinearInEasing.transform(colorTransitionFraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    var expandedDropdown by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    // TODO to solve the problem of the key map list jumping up when showing the bottom sheet:
    // Always show it behind the bottom nav bar and add the necessary padding at the end of the list. The bottom sheet
    // will then just replace the bottom sheet.

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                when (state) {
                    is KeyMapAppBarState.RootGroup -> AppBarStatus(
                        state = state,
                        onTogglePausedClick = onTogglePausedClick,
                    )

                    is KeyMapAppBarState.Selecting -> SelectedText(selectionCount = state.selectionCount)
                    is KeyMapAppBarState.ChildGroup -> TODO()
                }
            },
            navigationIcon = {
                AnimatedContent(state is KeyMapAppBarState.Selecting) { isSelecting ->
                    if (isSelecting) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.home_app_bar_cancel_selecting),
                            )
                        }
                    } else {
                        IconButton(onClick = onSortClick) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = stringResource(R.string.home_app_bar_sort),
                            )
                        }
                    }
                }
            },
            actions = {
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
            },
            colors = appBarColors,
        )
        AnimatedVisibility(state is KeyMapAppBarState.RootGroup && state.warnings.isNotEmpty()) {
            Surface(color = appBarContainerColor) {
                HomeWarningList(
                    modifier = Modifier.padding(bottom = 8.dp),
                    warnings = (state as? KeyMapAppBarState.RootGroup)?.warnings ?: emptyList(),
                    onFixClick = onFixWarningClick,
                )
            }
        }
    }
}

@Composable
private fun AppBarStatus(
    state: KeyMapAppBarState.RootGroup,
    onTogglePausedClick: () -> Unit,
) {
    val pausedButtonContainerColor by animateColorAsState(
        targetValue = if (state.isPaused || state.warnings.isNotEmpty()) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            LocalCustomColorsPalette.current.greenContainer
        },
    )

    val pausedButtonContentColor by animateColorAsState(
        targetValue = if (state.isPaused || state.warnings.isNotEmpty()) {
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

        if (state.isPaused) {
            buttonIcon = Icons.Rounded.PauseCircleOutline
            buttonText = stringResource(R.string.home_app_bar_status_paused)
        } else if (state.warnings.isNotEmpty()) {
            buttonIcon = Icons.Rounded.ErrorOutline
            buttonText = pluralStringResource(
                R.plurals.home_app_bar_status_warnings,
                state.warnings.size,
                state.warnings.size,
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
