package io.github.sds100.keymapper.base.actions.uielement

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.system.apps.ChooseAppScreen
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperDropdownMenu
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.WindowSizeClassExt.compareTo
import io.github.sds100.keymapper.base.utils.ui.compose.icons.AdGroup
import io.github.sds100.keymapper.base.utils.ui.compose.icons.JumpToElement
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.drawable
import io.github.sds100.keymapper.common.utils.NodeInteractionType
import io.github.sds100.keymapper.common.utils.State
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
                onSelectInteractionType = viewModel::onSelectElementInteractionType,
                onDescriptionChanged = viewModel::onDescriptionChanged,
            )
        }

        composable(DEST_SELECT_APP) {
            ChooseAppScreen(
                modifier = Modifier.fillMaxSize(),
                title = stringResource(R.string.action_interact_ui_element_choose_app_title),
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
                onAdditionalElementsCheckedChange = viewModel::onAdditionalElementsCheckedChanged,
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
    onSelectInteractionType: (NodeInteractionType) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
) {
    val snackbarHostState = SnackbarHostState()

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val widthSizeClass = windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass
    val heightSizeClass = windowAdaptiveInfo.windowSizeClass.windowHeightSizeClass

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
                if (selectedElementState == null || selectedElementState.description.isBlank()) {
                    DisabledExtendedFloatingActionButton(
                        icon = { Icon(Icons.Rounded.Check, stringResource(R.string.button_done)) },
                        text = stringResource(R.string.button_done),
                    )
                } else {
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

                if (heightSizeClass == WindowHeightSizeClass.COMPACT || widthSizeClass >= WindowWidthSizeClass.EXPANDED) {
                    Row {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxHeight()
                                .weight(1f),
                        ) {
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
                        }

                        if (selectedElementState != null) {
                            SelectedElementSection(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp)
                                    .weight(1f),
                                state = selectedElementState,
                                onSelectInteractionType = onSelectInteractionType,
                                onDescriptionChanged = onDescriptionChanged,
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SelectedElementSection(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                state = selectedElementState,
                                onSelectInteractionType = onSelectInteractionType,
                                onDescriptionChanged = onDescriptionChanged,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisabledExtendedFloatingActionButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: String,
) {
    Surface(
        modifier = modifier,
        shape = FloatingActionButtonDefaults.extendedFabShape,
        color = FloatingActionButtonDefaults.containerColor.copy(alpha = 0.5f),
    ) {
        Row(
            modifier =
            Modifier
                .sizeIn(minWidth = 80.dp, minHeight = 56.dp)
                .padding(start = 16.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            val contentColor =
                MaterialTheme.colorScheme.contentColorFor(FloatingActionButtonDefaults.containerColor)
                    .copy(alpha = 0.5f)

            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
                Spacer(Modifier.width(12.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                )
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
        when (val state = state) {
            is State.Data<RecordUiElementState> -> {
                val interactionCount: Int = when (val data = state.data) {
                    is RecordUiElementState.CountingDown -> data.interactionCount
                    is RecordUiElementState.Recorded -> data.interactionCount
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
                            R.plurals.action_interact_ui_element_elements_detected,
                            interactionCount,
                            interactionCount,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Text(
                        stringResource(R.string.action_interact_ui_element_choose_app_title),
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
private fun SelectedElementSection(
    modifier: Modifier = Modifier,
    state: SelectedUiElementState,
    onDescriptionChanged: (String) -> Unit = {},
    onSelectInteractionType: (NodeInteractionType) -> Unit = {},
) {
    var interactionTypeExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        val isError = state.description.isBlank()

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.description,
            onValueChange = onDescriptionChanged,
            isError = isError,
            maxLines = 1,
            singleLine = true,
            supportingText = if (isError) {
                { Text(stringResource(R.string.error_cant_be_empty)) }
            } else {
                null
            },
            label = {
                Text(stringResource(R.string.action_interact_ui_element_description_label))
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        OptionsHeaderRow(
            icon = Icons.Outlined.Info,
            text = stringResource(R.string.action_interact_ui_element_interaction_details_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.action_interact_ui_element_app_label),
            style = MaterialTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (state.appIcon != null) {
                val painter = rememberDrawablePainter(state.appIcon.drawable)
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painter,
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = state.appName, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.nodeText != null) {
            Text(
                text = stringResource(R.string.action_interact_ui_element_text_label),
                style = MaterialTheme.typography.titleSmall,
            )

            Text(text = state.nodeText, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.nodeToolTipHint != null) {
            Text(
                text = stringResource(R.string.action_interact_ui_element_tooltip_label),
                style = MaterialTheme.typography.titleSmall,
            )

            Text(text = state.nodeToolTipHint, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.nodeClassName != null) {
            Text(
                text = stringResource(R.string.action_interact_ui_element_class_name_label),
                style = MaterialTheme.typography.titleSmall,
            )

            Text(text = state.nodeClassName, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.nodeViewResourceId != null) {
            Text(
                text = stringResource(R.string.action_interact_ui_element_view_id_label),
                style = MaterialTheme.typography.titleSmall,
            )

            Text(text = state.nodeViewResourceId, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.nodeUniqueId != null) {
            Text(
                text = stringResource(R.string.action_interact_ui_element_unique_id_label),
                style = MaterialTheme.typography.titleSmall,
            )

            Text(text = state.nodeUniqueId, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OptionsHeaderRow(
            icon = KeyMapperIcons.JumpToElement,
            text = stringResource(R.string.action_interact_ui_element_interaction_type_dropdown),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.action_interact_ui_element_interaction_type_dropdown_caption),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        KeyMapperDropdownMenu(
            expanded = interactionTypeExpanded,
            onExpandedChange = { interactionTypeExpanded = it },
            values = state.interactionTypes,
            selectedValue = state.selectedInteraction,
            onValueChanged = onSelectInteractionType,
        )

        Spacer(modifier = Modifier.height(8.dp))
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
private fun PreviewLoading() {
    KeyMapperTheme {
        LandingScreen(
            recordState = State.Loading,
            selectedElementState = null,
        )
    }
}

@Composable
private fun selectedUiElementState(): SelectedUiElementState {
    val appIcon = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    return SelectedUiElementState(
        description = "Tap test node",
        packageName = "com.example.test",
        appName = "Test App",
        appIcon = ComposeIconInfo.Drawable(appIcon),
        nodeText = "Test Node",
        nodeToolTipHint = "Test tooltip",
        nodeClassName = "android.widget.ImageButton",
        nodeViewResourceId = "io.github.sds100.keymapper:id/menu_button",
        nodeUniqueId = "123",
        interactionTypes = listOf(NodeInteractionType.LONG_CLICK to "Tap and hold"),
        selectedInteraction = NodeInteractionType.LONG_CLICK,
    )
}

@Preview(device = Devices.PIXEL_7)
@Composable
private fun PreviewSelectedElementPortrait() {
    KeyMapperTheme {
        LandingScreen(
            recordState = State.Data(RecordUiElementState.Recorded(3)),
            selectedElementState = selectedUiElementState(),
        )
    }
}

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun PreviewSelectedElementLandscape() {
    KeyMapperTheme {
        LandingScreen(
            recordState = State.Data(RecordUiElementState.Recorded(3)),
            selectedElementState = selectedUiElementState(),
        )
    }
}
