package io.github.sds100.keymapper.mappings.keymaps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionsScreen
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.constraints.ConstraintsScreen
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerScreen
import kotlinx.coroutines.launch

@Composable
fun ConfigKeyMapScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigKeyMapViewModel,
    navigateBack: () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    ConfigKeyMapScreen(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        triggerScreen = {
            TriggerScreen(Modifier.fillMaxSize(), viewModel.configTriggerViewModel)
        },
        actionScreen = {
            ActionsScreen(Modifier.fillMaxSize(), viewModel.configActionsViewModel)
        },
        constraintsScreen = {
            ConstraintsScreen(Modifier.fillMaxSize(), viewModel.configConstraintsViewModel)
        },
        optionsScreen = {
            KeyMapOptionsScreen(
                Modifier.fillMaxSize(),
                viewModel.configTriggerViewModel.optionsViewModel,
            )
        },
        navigateBack = navigateBack,
        onDoneClick = {
            viewModel.save()
            navigateBack()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigKeyMapScreen(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass,
    triggerScreen: @Composable () -> Unit,
    actionScreen: @Composable () -> Unit,
    constraintsScreen: @Composable () -> Unit,
    optionsScreen: @Composable () -> Unit,
    navigateBack: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    // TODO help button on app bar depending on which tab is shown

    val scope = rememberCoroutineScope()

    BackHandler {
        // TODO
        navigateBack()
    }

    Scaffold(
        modifier.displayCutoutPadding(),
        bottomBar = {
            KeyMapAppBar(onBackClick = {
                // TODO back pressed dialog
            }, onDoneClick = onDoneClick)
        },
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding)) {
            val tabs = determineTabs(maxWidth, maxHeight)
            val isVerticalTwoScreen = maxWidth < 720.dp
            val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)

            Column(Modifier.fillMaxSize()) {
                if (tabs.size > 1) {
                    @Composable
                    fun Tabs() {
                        for ((index, tab) in tabs.withIndex()) {
                            Tab(
                                selected = pagerState.targetPage == index,
                                text = { Text(getTabTitle(tab), maxLines = 1) },
                                onClick = {
                                    scope.launch {
                                        pagerState.scrollToPage(
                                            tabs.indexOf(tab),
                                        )
                                    }
                                },
                            )
                        }
                    }

                    if (this@BoxWithConstraints.maxWidth < 500.dp) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = pagerState.targetPage,
                            modifier = Modifier.fillMaxWidth(),
                            divider = {},
                        ) {
                            Tabs()
                        }
                    } else {
                        PrimaryTabRow(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            selectedTabIndex = pagerState.targetPage,
                            divider = {},
                        ) {
                            Tabs()
                        }
                    }
                }

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                ) { pageIndex ->
                    when (tabs[pageIndex]) {
                        ConfigKeyMapTab.TRIGGER -> triggerScreen()
                        ConfigKeyMapTab.ACTIONS -> actionScreen()
                        ConfigKeyMapTab.CONSTRAINTS -> constraintsScreen()
                        ConfigKeyMapTab.OPTIONS -> optionsScreen()
                        ConfigKeyMapTab.TRIGGER_AND_ACTIONS -> {
                            if (isVerticalTwoScreen) {
                                VerticalTwoScreens(
                                    topTitle = stringResource(R.string.tab_trigger),
                                    topHelpUrl = stringResource(R.string.url_trigger_guide),
                                    topScreen = triggerScreen,
                                    bottomTitle = stringResource(R.string.tab_actions),
                                    bottomHelpUrl = stringResource(R.string.url_action_guide),
                                    bottomScreen = actionScreen,
                                )
                            } else {
                                HorizontalTwoScreens(
                                    leftTitle = stringResource(R.string.tab_trigger),
                                    leftHelpUrl = stringResource(R.string.url_trigger_guide),
                                    leftScreen = triggerScreen,
                                    rightTitle = stringResource(R.string.tab_actions),
                                    rightHelpUrl = stringResource(R.string.url_action_guide),
                                    rightScreen = actionScreen,
                                )
                            }
                        }

                        ConfigKeyMapTab.CONSTRAINTS_AND_OPTIONS -> {
                            if (isVerticalTwoScreen) {
                                VerticalTwoScreens(
                                    topTitle = stringResource(R.string.tab_constraints),
                                    topHelpUrl = stringResource(R.string.url_constraints_guide),
                                    topScreen = constraintsScreen,
                                    bottomTitle = stringResource(R.string.tab_options),
                                    bottomHelpUrl = stringResource(R.string.url_trigger_options_guide),
                                    bottomScreen = optionsScreen,
                                )
                            } else {
                                HorizontalTwoScreens(
                                    leftTitle = stringResource(R.string.tab_constraints),
                                    leftHelpUrl = stringResource(R.string.url_constraints_guide),
                                    leftScreen = constraintsScreen,
                                    rightTitle = stringResource(R.string.tab_options),
                                    rightHelpUrl = stringResource(R.string.url_trigger_options_guide),
                                    rightScreen = optionsScreen,
                                )
                            }
                        }

                        ConfigKeyMapTab.ALL -> FourScreens(
                            topLeftTitle = stringResource(R.string.tab_trigger),
                            topLeftHelpUrl = stringResource(R.string.url_trigger_guide),
                            topLeftScreen = triggerScreen,
                            topRightTitle = stringResource(R.string.tab_actions),
                            topRightHelpUrl = stringResource(R.string.url_action_guide),
                            topRightScreen = actionScreen,
                            bottomLeftTitle = stringResource(R.string.tab_constraints),
                            bottomLeftHelpUrl = stringResource(R.string.url_constraints_guide),
                            bottomLeftScreen = constraintsScreen,
                            bottomRightTitle = stringResource(R.string.tab_options),
                            bottomRightHelpUrl = stringResource(R.string.url_trigger_options_guide),
                            bottomRightScreen = optionsScreen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyMapAppBar(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    // TODO enabled switch
    // TODO only show help button if one screen per tab
    BottomAppBar(
        modifier = modifier,
        floatingActionButton = {
        },
        actions = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_go_back))
            }
        },
    )
}

@Composable
private fun VerticalTwoScreens(
    modifier: Modifier = Modifier,
    topTitle: String,
    topHelpUrl: String,
    topScreen: @Composable () -> Unit,
    bottomTitle: String,
    bottomHelpUrl: String,
    bottomScreen: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        ScreenCard(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            topTitle,
            topHelpUrl,
            topScreen,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ScreenCard(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            bottomTitle,
            bottomHelpUrl,
            bottomScreen,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HorizontalTwoScreens(
    modifier: Modifier = Modifier,
    leftTitle: String,
    leftHelpUrl: String,
    leftScreen: @Composable () -> Unit,
    rightTitle: String,
    rightHelpUrl: String,
    rightScreen: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.weight(1f)) {
            ScreenCard(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                leftTitle,
                leftHelpUrl,
                leftScreen,
            )
            Spacer(modifier = Modifier.width(8.dp))
            ScreenCard(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                rightTitle,
                rightHelpUrl,
                rightScreen,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun FourScreens(
    modifier: Modifier = Modifier,
    topLeftTitle: String,
    topLeftHelpUrl: String,
    topLeftScreen: @Composable () -> Unit,
    topRightTitle: String,
    topRightHelpUrl: String,
    topRightScreen: @Composable () -> Unit,
    bottomLeftTitle: String,
    bottomLeftHelpUrl: String,
    bottomLeftScreen: @Composable () -> Unit,
    bottomRightTitle: String,
    bottomRightHelpUrl: String,
    bottomRightScreen: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.weight(1f)) {
            ScreenCard(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                topLeftTitle,
                topLeftHelpUrl,
                topLeftScreen,
            )
            Spacer(modifier = Modifier.width(8.dp))
            ScreenCard(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                topRightTitle,
                topRightHelpUrl,
                topRightScreen,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.weight(1f)) {
            ScreenCard(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                bottomLeftTitle,
                bottomLeftHelpUrl,
                bottomLeftScreen,
            )
            Spacer(modifier = Modifier.width(8.dp))
            ScreenCard(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                bottomRightTitle,
                bottomRightHelpUrl,
                bottomRightScreen,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ScreenCard(
    modifier: Modifier = Modifier,
    title: String,
    helpUrl: String,
    screen: @Composable () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    OutlinedCard(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                IconButton(onClick = { uriHandler.openUri(helpUrl) }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.button_help),
                    )
                }
            }

            screen()
        }
    }
}

private fun determineTabs(maxWidth: Dp, maxHeight: Dp): List<ConfigKeyMapTab> {
    return when {
        maxWidth >= 800.dp && maxHeight >= 800.dp -> listOf(ConfigKeyMapTab.ALL)

        (maxWidth >= 1000.dp && maxHeight >= 450.dp) ||
            (maxWidth >= 450.dp && maxHeight >= 1000.dp) -> listOf(
            ConfigKeyMapTab.TRIGGER_AND_ACTIONS,
            ConfigKeyMapTab.CONSTRAINTS_AND_OPTIONS,
        )

        else -> listOf(
            ConfigKeyMapTab.TRIGGER,
            ConfigKeyMapTab.ACTIONS,
            ConfigKeyMapTab.CONSTRAINTS,
            ConfigKeyMapTab.OPTIONS,
        )
    }
}

@Composable
private fun getTabTitle(tab: ConfigKeyMapTab): String {
    return when (tab) {
        ConfigKeyMapTab.TRIGGER -> stringResource(R.string.tab_trigger)
        ConfigKeyMapTab.ACTIONS -> stringResource(R.string.tab_actions)
        ConfigKeyMapTab.CONSTRAINTS -> stringResource(R.string.tab_constraints)
        ConfigKeyMapTab.OPTIONS -> stringResource(R.string.tab_options)
        ConfigKeyMapTab.TRIGGER_AND_ACTIONS -> stringResource(R.string.tab_trigger_and_actions)
        ConfigKeyMapTab.CONSTRAINTS_AND_OPTIONS -> stringResource(R.string.tab_constraints_and_more)
        ConfigKeyMapTab.ALL -> ""
    }
}

private enum class ConfigKeyMapTab {
    TRIGGER,
    ACTIONS,
    CONSTRAINTS,
    OPTIONS,
    TRIGGER_AND_ACTIONS,
    CONSTRAINTS_AND_OPTIONS,
    ALL,
}

@Preview(device = Devices.PIXEL, showSystemUi = true)
@Composable
private fun SmallScreenPreview() {
    KeyMapperTheme {
        ConfigKeyMapScreen(
            modifier = Modifier.fillMaxSize(),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
            navigateBack = {},
        )
    }
}

@Preview(device = Devices.NEXUS_7_2013, showSystemUi = true)
@Composable
private fun MediumScreenPreview() {
    KeyMapperTheme {
        ConfigKeyMapScreen(
            modifier = Modifier.fillMaxSize(),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
            navigateBack = {},
        )
    }
}

@Preview(device = Devices.FOLDABLE, showSystemUi = true)
@Composable
private fun MediumScreenLandscapePreview() {
    KeyMapperTheme {
        ConfigKeyMapScreen(
            modifier = Modifier.fillMaxSize(),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
            navigateBack = {},
        )
    }
}

@Preview(device = Devices.NEXUS_10, showSystemUi = true)
@Composable
private fun LargeScreenPreview() {
    KeyMapperTheme {
        ConfigKeyMapScreen(
            modifier = Modifier.fillMaxSize(),
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
            navigateBack = {},
        )
    }
}
