package io.github.sds100.keymapper.actions.uielement

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo

@Composable
fun InteractUiElementScreen(
    modifier: Modifier = Modifier,
    viewModel: InteractUiElementViewModel,
    navigateBack: () -> Unit,
) {
    val snackbarHostState = SnackbarHostState()
    val recordState by viewModel.recordState.collectAsStateWithLifecycle()
    val selectedElementState by viewModel.selectedElementState.collectAsStateWithLifecycle()

    InteractUiElementScreen(
        modifier = modifier,
        recordState = recordState,
        selectedElementState = selectedElementState,
        onBackClick = navigateBack,
        onDoneClick = { viewModel.onDoneClick() },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun InteractUiElementScreen(
    modifier: Modifier = Modifier,
    recordState: State<RecordUiElementState>,
    selectedElementState: SelectedUiElementState?,
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
    BackHandler(onBack = onBackClick)

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
            }
        }
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        InteractUiElementScreen(
            recordState = State.Data(RecordUiElementState.Empty),
            selectedElementState = null,
        )
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    KeyMapperTheme {
        InteractUiElementScreen(
            recordState = State.Loading,
            selectedElementState = null,
        )
    }
}

@Preview
@Composable
private fun PreviewSelectedElement() {
    val appIcon = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    KeyMapperTheme {
        InteractUiElementScreen(
            recordState = State.Data(RecordUiElementState.Recorded(0)),
            selectedElementState = SelectedUiElementState(
                description = "Test",
                appName = "Test App",
                appIcon = ComposeIconInfo.Drawable(appIcon),
                nodeText = "Test Node",
                nodeClassName = "Test Class",
                nodeViewResourceId = "io.github.sds100.keymapper:id/menu_button",
                nodeUniqueId = "123",
                interactionTypes = listOf(),
                selectedInteraction = 0,
            ),
        )
    }
}
