package io.github.sds100.keymapper.base.keymaps

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canopas.lib.showcase.IntroShowcase
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionsScreen
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConstraintsScreen
import io.github.sds100.keymapper.base.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.base.trigger.TriggerScreen
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperTapTarget
import io.github.sds100.keymapper.base.utils.ui.compose.keyMapperShowcaseStyle
import io.github.sds100.keymapper.base.utils.ui.compose.openUriSafe
import kotlinx.coroutines.launch

@Composable
fun ConfigKeyMapScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigKeyMapViewModel,
    navigateBack: () -> Unit,
) {
    val isKeyMapEnabled by viewModel.isEnabled.collectAsStateWithLifecycle()
    val showActionTapTarget by viewModel.showActionsTapTarget.collectAsStateWithLifecycle()
    val showConstraintTapTarget by viewModel.showConstraintsTapTarget.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showBackDialog by rememberSaveable { mutableStateOf(false) }

    if (showBackDialog) {
        BackDialog(
            onDismiss = { showBackDialog = false },
            onDiscardClick = {
                showBackDialog = false
                navigateBack()
            },
        )
    }

    ConfigKeyMapScreen(
        modifier = modifier,
        isKeyMapEnabled = isKeyMapEnabled,
        onKeyMapEnabledChange = viewModel::onEnabledChanged,
        triggerScreen = {
            TriggerScreen(Modifier.fillMaxSize(), viewModel.configTriggerViewModel)
        },
        actionScreen = {
            ActionsScreen(Modifier.fillMaxSize(), viewModel.configActionsViewModel)
        },
        constraintsScreen = {
            ConstraintsScreen(
                Modifier.fillMaxSize(),
                viewModel.configConstraintsViewModel,
                snackbarHostState,
            )
        },
        optionsScreen = {
            KeyMapOptionsScreen(
                Modifier.fillMaxSize(),
                viewModel.configTriggerViewModel.optionsViewModel,
            )
        },
        onBackClick = {
            if (viewModel.isKeyMapEdited) {
                showBackDialog = true
            } else {
                navigateBack()
            }
        },
        onDoneClick = {
            viewModel.save()
            navigateBack()
        },
        snackbarHostState = snackbarHostState,
        showActionTapTarget = showActionTapTarget,
        onActionTapTargetCompleted = viewModel::onActionTapTargetCompleted,
        showConstraintTapTarget = showConstraintTapTarget,
        onConstraintTapTargetCompleted = viewModel::onConstraintTapTargetCompleted,
        onSkipTutorialClick = viewModel::onSkipTutorialClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigKeyMapScreen(
    modifier: Modifier = Modifier,
    isKeyMapEnabled: Boolean,
    onKeyMapEnabledChange: (Boolean) -> Unit = {},
    triggerScreen: @Composable () -> Unit,
    actionScreen: @Composable () -> Unit,
    constraintsScreen: @Composable () -> Unit,
    optionsScreen: @Composable () -> Unit,
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    showActionTapTarget: Boolean = false,
    onActionTapTargetCompleted: () -> Unit = {},
    showConstraintTapTarget: Boolean = false,
    onConstraintTapTargetCompleted: () -> Unit = {},
    onSkipTutorialClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val triggerHelpUrl = stringResource(R.string.url_trigger_guide)
    val actionsHelpUrl = stringResource(R.string.url_action_guide)
    val constraintsHelpUrl = stringResource(R.string.url_constraints_guide)
    val optionsHelpUrl = stringResource(R.string.url_trigger_options_guide)

    var currentTab: ConfigKeyMapTab? by remember { mutableStateOf(null) }
    val uriHandler = LocalUriHandler.current
    val ctx = LocalContext.current

    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier.displayCutoutPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ConfigKeyMapAppBar(
                isKeyMapEnabled = isKeyMapEnabled,
                onKeyMapEnabledChange = onKeyMapEnabledChange,
                onBackClick = onBackClick,
                onDoneClick = onDoneClick,
                showHelpButton = currentTab == ConfigKeyMapTab.TRIGGER ||
                    currentTab == ConfigKeyMapTab.ACTIONS ||
                    currentTab == ConfigKeyMapTab.CONSTRAINTS ||
                    currentTab == ConfigKeyMapTab.OPTIONS,
                onHelpClick = {
                    val url = when (currentTab) {
                        ConfigKeyMapTab.TRIGGER -> triggerHelpUrl
                        ConfigKeyMapTab.ACTIONS -> actionsHelpUrl
                        ConfigKeyMapTab.CONSTRAINTS -> constraintsHelpUrl
                        ConfigKeyMapTab.OPTIONS -> optionsHelpUrl
                        else -> return@ConfigKeyMapAppBar
                    }

                    if (url.isNotEmpty()) {
                        uriHandler.openUriSafe(ctx, url)
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding)) {
            val tabs = determineTabs(maxWidth, maxHeight)
            val isVerticalTwoScreen = maxWidth < 720.dp
            val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)
            currentTab = tabs.getOrNull(pagerState.targetPage)

            Column(Modifier.fillMaxSize()) {
                if (tabs.size > 1) {
                    @Composable
                    fun Tabs() {
                        for ((index, tab) in tabs.withIndex()) {
                            val tapTarget: OnboardingTapTarget? = when {
                                showActionTapTarget && tab == ConfigKeyMapTab.ACTIONS -> OnboardingTapTarget.CHOOSE_ACTION
                                showConstraintTapTarget && (tab == ConfigKeyMapTab.CONSTRAINTS || tab == ConfigKeyMapTab.CONSTRAINTS_AND_OPTIONS) -> OnboardingTapTarget.CHOOSE_CONSTRAINT
                                else -> null
                            }

                            IntroShowcase(
                                showIntroShowCase = tapTarget != null,
                                onShowCaseCompleted = if (tapTarget == OnboardingTapTarget.CHOOSE_ACTION) onActionTapTargetCompleted else onConstraintTapTargetCompleted,
                                dismissOnClickOutside = true,
                            ) {
                                var tabModifier: Modifier = Modifier

                                if (tapTarget != null) {
                                    tabModifier = tabModifier.introShowCaseTarget(
                                        index = 0,
                                        style = keyMapperShowcaseStyle(),
                                    ) {
                                        KeyMapperTapTarget(
                                            tapTarget = tapTarget,
                                            onSkipClick = onSkipTutorialClick,
                                        )
                                    }
                                }

                                Tab(
                                    modifier = tabModifier,
                                    selected = pagerState.targetPage == index,
                                    text = {
                                        Text(
                                            text = getTabTitle(tab),
                                            maxLines = 1,
                                        )
                                    },
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                tabs.indexOf(tab),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (this@BoxWithConstraints.maxWidth < 500.dp) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = pagerState.targetPage,
                            divider = {},
                            edgePadding = 16.dp,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Tabs()
                        }
                    } else {
                        PrimaryTabRow(
                            selectedTabIndex = pagerState.targetPage,
                            divider = {},
                            contentColor = MaterialTheme.colorScheme.onSurface,
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
                                    topHelpUrl = triggerHelpUrl,
                                    topScreen = triggerScreen,
                                    bottomTitle = stringResource(R.string.tab_actions),
                                    bottomHelpUrl = actionsHelpUrl,
                                    bottomScreen = actionScreen,
                                )
                            } else {
                                HorizontalTwoScreens(
                                    leftTitle = stringResource(R.string.tab_trigger),
                                    leftHelpUrl = triggerHelpUrl,
                                    leftScreen = triggerScreen,
                                    rightTitle = stringResource(R.string.tab_actions),
                                    rightHelpUrl = actionsHelpUrl,
                                    rightScreen = actionScreen,
                                )
                            }
                        }

                        ConfigKeyMapTab.CONSTRAINTS_AND_OPTIONS -> {
                            if (isVerticalTwoScreen) {
                                VerticalTwoScreens(
                                    topTitle = stringResource(R.string.tab_constraints),
                                    topHelpUrl = constraintsHelpUrl,
                                    topScreen = constraintsScreen,
                                    bottomTitle = stringResource(R.string.tab_options),
                                    bottomHelpUrl = optionsHelpUrl,
                                    bottomScreen = optionsScreen,
                                )
                            } else {
                                HorizontalTwoScreens(
                                    leftTitle = stringResource(R.string.tab_constraints),
                                    leftHelpUrl = constraintsHelpUrl,
                                    leftScreen = constraintsScreen,
                                    rightTitle = stringResource(R.string.tab_options),
                                    rightHelpUrl = optionsHelpUrl,
                                    rightScreen = optionsScreen,
                                )
                            }
                        }

                        ConfigKeyMapTab.ALL -> FourScreens(
                            topLeftTitle = stringResource(R.string.tab_trigger),
                            topLeftHelpUrl = triggerHelpUrl,
                            topLeftScreen = triggerScreen,
                            topRightTitle = stringResource(R.string.tab_actions),
                            topRightHelpUrl = actionsHelpUrl,
                            topRightScreen = actionScreen,
                            bottomLeftTitle = stringResource(R.string.tab_constraints),
                            bottomLeftHelpUrl = constraintsHelpUrl,
                            bottomLeftScreen = constraintsScreen,
                            bottomRightTitle = stringResource(R.string.tab_options),
                            bottomRightHelpUrl = optionsHelpUrl,
                            bottomRightScreen = optionsScreen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigKeyMapAppBar(
    modifier: Modifier = Modifier,
    isKeyMapEnabled: Boolean,
    onKeyMapEnabledChange: (Boolean) -> Unit = {},
    showHelpButton: Boolean,
    onHelpClick: () -> Unit,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    BottomAppBar(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onDoneClick,
                text = { Text(stringResource(R.string.button_done)) },
                icon = {
                    Icon(Icons.Rounded.Check, stringResource(R.string.button_done))
                },
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            )
        },
        actions = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_go_back))
            }

            Spacer(modifier = Modifier.width(8.dp))

            val text = if (isKeyMapEnabled) {
                stringResource(R.string.switch_enabled)
            } else {
                stringResource(R.string.switch_disabled)
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isKeyMapEnabled,
                onCheckedChange = onKeyMapEnabledChange,
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showHelpButton) {
                IconButton(onClick = onHelpClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.HelpOutline,
                        stringResource(R.string.action_help),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
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
    val ctx = LocalContext.current

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

                IconButton(onClick = { uriHandler.openUriSafe(ctx, helpUrl) }) {
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

@Composable
private fun BackDialog(
    onDismiss: () -> Unit,
    onDiscardClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_unsaved_changes)) },
        text = { Text(stringResource(R.string.dialog_message_unsaved_changes)) },
        confirmButton = {
            TextButton(onClick = onDiscardClick) { Text(stringResource(R.string.pos_discard_changes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.neg_keep_editing)) }
        },
    )
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
            isKeyMapEnabled = false,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
        )
    }
}

@Preview(device = Devices.NEXUS_7_2013, showSystemUi = true)
@Composable
private fun MediumScreenPreview() {
    KeyMapperTheme {
        ConfigKeyMapScreen(
            modifier = Modifier.fillMaxSize(),
            isKeyMapEnabled = true,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
        )
    }
}

@Preview(device = Devices.FOLDABLE, showSystemUi = true)
@Composable
private fun MediumScreenLandscapePreview() {
    KeyMapperTheme {
        ConfigKeyMapScreen(
            modifier = Modifier.fillMaxSize(),
            isKeyMapEnabled = true,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
        )
    }
}

@Preview(device = Devices.NEXUS_10, showSystemUi = true)
@Composable
private fun LargeScreenPreview() {
    KeyMapperTheme {
        ConfigKeyMapScreen(
            modifier = Modifier.fillMaxSize(),
            isKeyMapEnabled = true,
            triggerScreen = {},
            actionScreen = {},
            constraintsScreen = {},
            optionsScreen = {},
        )
    }
}
