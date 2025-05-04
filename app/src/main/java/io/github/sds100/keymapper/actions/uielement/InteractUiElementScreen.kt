package io.github.sds100.keymapper.actions.uielement

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.system.apps.ChooseAppScreen
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.compose.icons.AdGroup
import io.github.sds100.keymapper.util.ui.compose.icons.KeyMapperIcons
import kotlinx.coroutines.flow.update

private const val DEST_LANDING = "landing"
private const val DEST_SELECT_APP = "select_app"
private const val DEST_SELECT_ELEMENT = "select_element"

@Composable
fun InteractUiElementScreen(
    modifier: Modifier = Modifier,
    viewModel: InteractUiElementViewModel,
    navigateBack: () -> Unit,
) {
    val navController = rememberNavController()

    val recordState by viewModel.recordState.collectAsStateWithLifecycle()
    val selectedElementState by viewModel.selectedElementState.collectAsStateWithLifecycle()

    val appListState by viewModel.filteredAppListItems.collectAsStateWithLifecycle()
    val appSearchQuery by viewModel.appSearchQuery.collectAsStateWithLifecycle()

    val elementListState by viewModel.selectUiElementState.collectAsStateWithLifecycle()
    val elementSearchQuery by viewModel.elementSearchQuery.collectAsStateWithLifecycle()

    val onBackClick = {
        if (!navController.navigateUp()) {
            navigateBack()
        }
    }

    BackHandler(onBack = onBackClick)

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = DEST_LANDING,
        enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
        popEnterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
        popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
    ) {
        composable(DEST_LANDING) {
            LandingScreen(
                modifier = Modifier.fillMaxSize(),
                recordState = recordState,
                selectedElementState = selectedElementState,
                onRecordClick = viewModel::onRecordClick,
                onBackClick = onBackClick,
                onDoneClick = viewModel::onDoneClick,
                openSelectAppScreen = {
                    navController.navigate(DEST_SELECT_APP)
                },
            )
        }

        composable(DEST_SELECT_APP) {
            ChooseAppScreen(
                modifier = Modifier.fillMaxSize(),
                title = stringResource(R.string.action_interact_ui_element_choose_element_title),
                state = appListState,
                query = appSearchQuery,
                onQueryChange = { query -> viewModel.appSearchQuery.update { query } },
                onCloseSearch = { viewModel.appSearchQuery.update { null } },
                onNavigateBack = onBackClick,
                onClickApp = {
                    viewModel.onSelectApp(it)
                    navController.navigate(DEST_SELECT_ELEMENT)
                },
            )
        }

        composable(DEST_SELECT_ELEMENT) {
            ChooseElementScreen(
                modifier = Modifier.fillMaxSize(),
                state = elementListState,
                query = elementSearchQuery,
                onCloseSearch = { viewModel.elementSearchQuery.update { null } },
                onNavigateBack = onBackClick,
                onQueryChange = { query -> viewModel.elementSearchQuery.update { query } },
                onClickElement = {
                    viewModel.onSelectElement(it)
                    navController.popBackStack(route = DEST_LANDING, inclusive = false)
                },
                onSelectInteractionType = viewModel::onSelectInteractionTypeFilter,
            )
        }
    }
}

@Composable
private fun LandingScreen(
    modifier: Modifier = Modifier,
    recordState: State<RecordUiElementState>,
    selectedElementState: SelectedUiElementState?,
    onRecordClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    openSelectAppScreen: () -> Unit = {},
) {
    val snackbarHostState = SnackbarHostState()

    Scaffold(
        modifier.displayCutoutPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(actions = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.action_go_back),
                    )
                }
            }, floatingActionButton = {
                if (selectedElementState != null) {
                    ExtendedFloatingActionButton(
                        onClick = onDoneClick,
                        text = { Text(stringResource(R.string.button_done)) },
                        icon = {
                            Icon(Icons.Rounded.Check, stringResource(R.string.button_done))
                        },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    )
                }
            })
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = innerPadding.calculateStartPadding(layoutDirection)
        val endPadding = innerPadding.calculateEndPadding(layoutDirection)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = startPadding,
                    end = endPadding,
                ),
        ) {
            Column {
                Text(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp,
                    ),
                    text = stringResource(R.string.action_interact_ui_element_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = stringResource(R.string.action_interact_ui_element_description),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                RecordingSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = recordState,
                    onRecordClick = onRecordClick,
                    openSelectAppScreen = openSelectAppScreen,
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedElementState != null) {
                    SelectedElementSection(modifier = Modifier.fillMaxWidth(), selectedElementState)
                }
            }
        }
    }
}

@Composable
private fun RecordingSection(
    modifier: Modifier = Modifier,
    state: State<RecordUiElementState>,
    onRecordClick: () -> Unit = {},
    openSelectAppScreen: () -> Unit = {},
) {
    Column(modifier = modifier) {
        when (state) {
            is State.Data<RecordUiElementState> -> {
                val interactionCount: Int = when (state.data) {
                    is RecordUiElementState.CountingDown -> state.data.interactionCount
                    is RecordUiElementState.Recorded -> state.data.interactionCount
                    RecordUiElementState.Empty -> 0
                }

                InteractionCountBox(
                    modifier = Modifier.fillMaxWidth(),
                    interactionCount = interactionCount,
                    onClick = openSelectAppScreen,
                )

                Spacer(modifier = Modifier.height(8.dp))

                RecordButton(
                    modifier = Modifier.fillMaxWidth(),
                    state = state.data,
                    onClick = onRecordClick,
                )
            }

            State.Loading -> {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InteractionCountBox(
    modifier: Modifier = Modifier,
    interactionCount: Int,
    onClick: () -> Unit,
) {
    val enabled = interactionCount > 0

    val color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides color,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = KeyMapperIcons.AdGroup, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        pluralStringResource(
                            R.plurals.action_interact_ui_element_interactions_detected,
                            interactionCount,
                            interactionCount,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Text(
                        stringResource(R.string.action_interact_ui_element_choose_interaction),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun RecordButton(
    modifier: Modifier,
    state: RecordUiElementState,
    onClick: () -> Unit,
) {
    val text: String = when (state) {
        is RecordUiElementState.Empty -> stringResource(R.string.action_interact_ui_element_start_recording)
        is RecordUiElementState.Recorded -> stringResource(R.string.action_interact_ui_element_record_again)
        is RecordUiElementState.CountingDown -> stringResource(
            R.string.action_interact_ui_element_stop_recording,
            state.timeRemaining,
        )
    }

    if (state is RecordUiElementState.Recorded) {
        OutlinedButton(
            modifier = modifier,
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors().copy(
                contentColor = LocalCustomColorsPalette.current.red,
            ),
            border = BorderStroke(1.dp, color = LocalCustomColorsPalette.current.red),
        ) {
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        FilledTonalButton(
            modifier = modifier,
            onClick = onClick,
            colors = ButtonDefaults.filledTonalButtonColors().copy(
                containerColor = LocalCustomColorsPalette.current.red,
                contentColor = LocalCustomColorsPalette.current.onRed,
            ),
        ) {
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SelectedElementSection(modifier: Modifier = Modifier, state: SelectedUiElementState) {
    Column(modifier = modifier) {
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        LandingScreen(
            recordState = State.Data(RecordUiElementState.Empty),
            selectedElementState = null,
        )
    }
}

@Preview
@Composable
private fun PreviewSelectedElement() {
    val appIcon = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    KeyMapperTheme {
        LandingScreen(
            recordState = State.Data(RecordUiElementState.Recorded(3)),
            selectedElementState = SelectedUiElementState(
                description = "Test",
                appName = "Test App",
                appIcon = ComposeIconInfo.Drawable(appIcon),
                nodeText = "Test Node",
                nodeClassName = "android.widget.ImageButton",
                nodeViewResourceId = "io.github.sds100.keymapper:id/menu_button",
                nodeUniqueId = "123",
                interactionTypes = listOf(),
                selectedInteraction = NodeInteractionType.LONG_CLICK,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    KeyMapperTheme {
        LandingScreen(
            recordState = State.Loading,
            selectedElementState = null,
        )
    }
}
