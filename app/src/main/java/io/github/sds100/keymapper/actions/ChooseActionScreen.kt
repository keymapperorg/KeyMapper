package io.github.sds100.keymapper.actions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.util.ui.CustomDialog
import io.github.sds100.keymapper.util.ui.Icon
import io.github.sds100.keymapper.util.ui.SimpleListItem

/**
 * Created by sds100 on 12/07/2022.
 */

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
private fun ChooseActionScreenPreview() {
    MaterialTheme {
        ChooseActionScreen(
            actions = listOf(
                ChooseActionListItem(
                    ActionId.SWITCH_KEYBOARD,
                    "Switch keyboard",
                    Icon.ImageVector(Icons.Outlined.Keyboard)
                )
            ),
            query = ""
        )
    }
}

@Composable
fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseActionViewModel2 = hiltViewModel(),
    setResult: (ActionData) -> Unit,
    onBack: () -> Unit
) {
    ChooseActionScreen(
        modifier,
        viewModel.listItems,
        viewModel.query,
        onSearchQueryChanged = { viewModel.query = it },
        viewModel.configActionState,
        viewModel::dismissConfiguringAction,
        onActionConfigured = {
            setResult(it)
            onBack()
        },
        viewModel::onActionClick,
        onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    actions: List<ChooseActionListItem>,
    query: String,
    onSearchQueryChanged: (String) -> Unit = {},
    configActionState: ConfigActionState? = null,
    onDismissConfiguringAction: () -> Unit = {},
    onActionConfigured: (ActionData) -> Unit = {},
    onActionClick: (ActionId) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(modifier, bottomBar = {
        ChooseActionAppBar(onBack, query, onSearchQueryChanged)
    }) { padding ->

        when (configActionState) {
            is ConfigActionState.ChooseKeyboard -> {

                var selectedIme: ImeInfo? by remember { mutableStateOf(null) }

                CustomDialog(
                    onDismissRequest = onDismissConfiguringAction,
                    title = "Choose a keyboard",
                    confirmButton = {
                        TextButton(onClick = {
                            onActionConfigured(ActionData.SwitchKeyboard(selectedIme!!.id, selectedIme!!.label))
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState
    ) {
        items(listItems) {
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                icon = it.icon,
                title = it.title,
                onClick = { onActionClick(it.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseActionAppBar(onBack: () -> Unit, searchQuery: String, onSearchQueryChanged: (String) -> Unit) {
    var isSearching: Boolean by rememberSaveable { mutableStateOf(false) }

    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (isSearching) {
                    isSearching = false
                } else {
                    onBack()
                }
            }) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.choose_action_back_content_description)
                )
            }

            AnimatedVisibility(visible = isSearching, modifier = Modifier.fillMaxWidth()) {
                TextField(value = searchQuery, onValueChange = onSearchQueryChanged)
            }

            AnimatedVisibility(visible = !isSearching) {
                FloatingActionButton(
                    onClick = { isSearching = true }, elevation = BottomAppBarDefaults.FloatingActionButtonElevation
                ) {
                    if (isSearching) {
                        Icon(
                            Icons.Outlined.Done,
                            contentDescription = stringResource(R.string.choose_action_done_searching_content_description)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.choose_action_search_content_description)
                        )
                    }
                }
            }
        }
    }
}