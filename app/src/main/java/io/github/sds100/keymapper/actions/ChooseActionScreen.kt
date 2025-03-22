package io.github.sds100.keymapper.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.compose.SimpleListItem
import io.github.sds100.keymapper.util.ui.compose.SimpleListItemGroup
import io.github.sds100.keymapper.util.ui.compose.SimpleListItemHeader
import io.github.sds100.keymapper.util.ui.compose.SimpleListItemModel

@Composable
fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseActionViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.groups.collectAsStateWithLifecycle()
    ChooseActionScreen(
        modifier = modifier,
        state = state,
        onClickAction = viewModel::onListItemClick,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    state: State<List<SimpleListItemGroup>>,
    onClickAction: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomAppBar(actions = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.bottom_app_bar_back_content_description),
                    )
                }
                // TODO search view
            })
        },
    ) { contentPadding ->
        Surface(Modifier.padding(contentPadding)) {
            Column {
                Text(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp,
                    ),
                    text = stringResource(R.string.choose_action_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                when (state) {
                    State.Loading -> LoadingScreen(Modifier.fillMaxSize())

                    is State.Data -> ListScreen(
                        modifier = Modifier.fillMaxSize(),
                        groups = state.data,
                        onClickAction = onClickAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ListScreen(
    modifier: Modifier = Modifier,
    groups: List<SimpleListItemGroup>,
    onClickAction: (String) -> Unit,
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (group in groups) {
            stickyHeader {
                SimpleListItemHeader(modifier = Modifier.fillMaxWidth(), text = group.header)
            }

            items(group.items) { model ->
                SimpleListItem(
                    modifier = Modifier.fillMaxWidth(),
                    model = model,
                    onClick = { onClickAction(model.id) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewList() {
    KeyMapperTheme {
        ChooseActionScreen(
            state = State.Data(
                listOf(
                    SimpleListItemGroup(
                        header = "Apps",
                        items = listOf(
                            SimpleListItemModel(
                                "app",
                                title = "Launch app",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                            ),
                            SimpleListItemModel(
                                "app",
                                title = "Launch app shortcut",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                            ),

                        ),
                    ),
                    SimpleListItemGroup(
                        header = "Connectivity",
                        items = listOf(
                            SimpleListItemModel(
                                "app",
                                title = "Enable WiFi",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Wifi),
                                subtitle = "Requires root",
                                isSubtitleError = true,
                            ),
                            SimpleListItemModel(
                                "bluetooth",
                                title = "Toggle Bluetooth",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Bluetooth),
                                subtitle = "Requires root",
                                isSubtitleError = true,
                                isEnabled = false,
                            ),

                        ),
                    ),

                ),
            ),
        )
    }
}

// TODO show as grid
@Preview(device = Devices.TABLET)
@Composable
private fun PreviewGrid() {
    KeyMapperTheme {
        ChooseActionScreen(
            state = State.Data(
                listOf(
                    SimpleListItemGroup(
                        header = "Apps",
                        items = listOf(
                            SimpleListItemModel(
                                "app",
                                title = "Launch app",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                            ),
                            SimpleListItemModel(
                                "app",
                                title = "Launch app shortcut",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                            ),

                        ),
                    ),
                    SimpleListItemGroup(
                        header = "Connectivity",
                        items = listOf(
                            SimpleListItemModel(
                                "app",
                                title = "Enable WiFi",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Wifi),
                                subtitle = "Requires root",
                                isSubtitleError = true,
                            ),
                            SimpleListItemModel(
                                "bluetooth",
                                title = "Toggle Bluetooth",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Bluetooth),
                                subtitle = "Requires root",
                                isSubtitleError = true,
                                isEnabled = false,
                            ),

                        ),
                    ),

                ),
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    KeyMapperTheme {
        ChooseActionScreen(
            state = State.Loading,
        )
    }
}
