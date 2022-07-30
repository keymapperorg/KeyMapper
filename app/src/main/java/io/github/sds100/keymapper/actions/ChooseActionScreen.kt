package io.github.sds100.keymapper.actions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.destinations.ChooseKeyCodeScreenDestination
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.util.ui.*

/**
 * Created by sds100 on 12/07/2022.
 */

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
private fun ChooseActionScreenPreview() {
    MaterialTheme {
        ChooseActionScreen(
            actions = listOf(
                ChooseActionListItem.Header("Keyboard"),
                ChooseActionListItem.Action(
                    ActionId.SWITCH_KEYBOARD,
                    "Switch keyboard",
                    KMIcon.ImageVector(Icons.Outlined.Keyboard)
                ),
                ChooseActionListItem.Action(
                    ActionId.SHOW_KEYBOARD,
                    "Show keyboard",
                    KMIcon.ImageVector(Icons.Outlined.Keyboard)
                )
            ),
            searchState = SearchState.Idle
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
private fun ChooseActionScreenPreview_Searching() {
    MaterialTheme {
        ChooseActionScreen(
            actions = listOf(
                ChooseActionListItem.Header("Keyboard"),
                ChooseActionListItem.Action(
                    ActionId.SWITCH_KEYBOARD,
                    "Switch keyboard",
                    KMIcon.ImageVector(Icons.Outlined.Keyboard)
                )
            ),
            searchState = SearchState.Searching("Search")
        )
    }
}

@RootNavGraph(start = true)
@Destination
@Composable
fun ChooseActionScreen(
    viewModel: ChooseActionViewModel2,
    navigator: DestinationsNavigator,
    keyCodeResultRecipient: ResultRecipient<ChooseKeyCodeScreenDestination, Int>,
    setResult: (ActionData) -> Unit,
    navigateBack: () -> Unit
) {
    keyCodeResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                viewModel.dismissConfiguringAction()
            }

            is NavResult.Value -> {
                viewModel.onChooseKeyCode(result.value)
            }
        }
    }

    ChooseActionScreen(
        viewModel = viewModel,
        setResult = setResult,
        onBack = navigateBack,
        navigateToChooseKeyCode = {
            viewModel.onNavigateToConfigScreen()
            navigator.navigate(ChooseKeyCodeScreenDestination)
        }
    )
}

@Composable
private fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseActionViewModel2,
    setResult: (ActionData) -> Unit,
    onBack: () -> Unit,
    navigateToChooseKeyCode: () -> Unit
) {
    val searchState by viewModel.searchState.collectAsState()

    ChooseActionScreen(
        modifier = modifier,
        actions = viewModel.actionListItems,
        searchState = searchState,
        setSearchState = viewModel::setSearchState,
        configActionState = viewModel.configActionState,
        onDismissConfiguringAction = viewModel::dismissConfiguringAction,
        onActionConfigured = {
            setResult(it)
            onBack()
        },
        onActionClick = viewModel::onActionClick,
        onBack = onBack,
        navigateToChooseKeyCode = navigateToChooseKeyCode
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    actions: List<ChooseActionListItem>,
    searchState: SearchState,
    setSearchState: (SearchState) -> Unit = {},
    configActionState: ConfigActionState? = null,
    onDismissConfiguringAction: () -> Unit = {},
    onActionConfigured: (ActionData) -> Unit = {},
    onActionClick: (ActionId) -> Unit = {},
    onBack: () -> Unit = {},
    navigateToChooseKeyCode: () -> Unit = {}
) {
    Scaffold(modifier, bottomBar = {
        SearchAppBar(onBack, searchState, setSearchState) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.choose_action_back_content_description)
                )
            }
        }
    }) { padding ->

        if (configActionState is ConfigActionState.Finished) {
            onActionConfigured(configActionState.action)
        }

        if (configActionState is ConfigActionState.Screen) {
            when (configActionState) {
                is ConfigActionState.Screen.ChooseKeycode -> navigateToChooseKeyCode()

                ConfigActionState.Screen.Navigated -> {}
            }
        }

        if (configActionState is ConfigActionState.Dialog) {
            when (configActionState) {
                is ConfigActionState.Dialog.ChooseKeyboard -> {

                    var selectedIme: ImeInfo? by remember { mutableStateOf(null) }

                    CustomDialog(
                        onDismissRequest = onDismissConfiguringAction,
                        title = "Choose a keyboard",
                        confirmButton = {
                            TextButton(onClick = {
                            }, enabled = selectedIme != null) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = onDismissConfiguringAction) {
                                Text("Cancel")
                            }
                        }) {
                        LazyColumn {
                            items(configActionState.inputMethods) { imeInfo ->
                                Row {
                                    RadioButton(selected = selectedIme == imeInfo, onClick = { selectedIme = imeInfo })
                                    Text(imeInfo.label)
                                }
                            }
                        }
                    }
                }
            }
        }

        ChooseActionList(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            actions,
            onActionClick
        )
    }
}

@Composable
private fun ChooseActionList(
    modifier: Modifier = Modifier,
    listItems: List<ChooseActionListItem>,
    onActionClick: (ActionId) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        state = listState
    ) {

        items(
            listItems,
            key = { listItem ->
                when (listItem) {
                    is ChooseActionListItem.Action -> listItem.id
                    is ChooseActionListItem.Header -> listItem.header
                }
            },
            contentType = { listItem ->
                when (listItem) {
                    is ChooseActionListItem.Action -> 0
                    is ChooseActionListItem.Header -> 1
                }
            }
        ) { listItem ->
            when (listItem) {
                is ChooseActionListItem.Action ->
                    SimpleListItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = listItem.icon,
                        title = listItem.title,
                        onClick = { onActionClick(listItem.id) }
                    )

                is ChooseActionListItem.Header ->
                    HeaderListItem(
                        modifier = Modifier.fillMaxWidth(),
                        text = listItem.header
                    )
            }
        }
    }
}