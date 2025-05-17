package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.common.util.state.State
import io.github.sds100.keymapper.base.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.util.ui.compose.SearchAppBarActions
import io.github.sds100.keymapper.base.util.ui.compose.SimpleListItemFixedHeight
import io.github.sds100.keymapper.base.util.ui.compose.SimpleListItemGroup
import io.github.sds100.keymapper.base.util.ui.compose.SimpleListItemHeader
import io.github.sds100.keymapper.base.util.ui.compose.SimpleListItemModel
import kotlinx.coroutines.flow.update

@Composable
fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseActionViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.groups.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    EnableFlashlightActionBottomSheet(viewModel.createActionDelegate)
    ChangeFlashlightStrengthActionBottomSheet(viewModel.createActionDelegate)
    HttpRequestBottomSheet(viewModel.createActionDelegate)

    ChooseActionScreen(
        modifier = modifier,
        state = state,
        query = query,
        onQueryChange = { newQuery -> viewModel.searchQuery.update { newQuery } },
        onCloseSearch = { viewModel.searchQuery.update { null } },
        onClickAction = viewModel::onListItemClick,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseActionScreen(
    modifier: Modifier = Modifier,
    state: State<List<SimpleListItemGroup>>,
    query: String? = null,
    onQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onClickAction: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.imePadding(),
                actions = {
                    SearchAppBarActions(
                        onCloseSearch = onCloseSearch,
                        onNavigateBack = onNavigateBack,
                        onQueryChange = onQueryChange,
                        enabled = state is State.Data,
                        query = query,
                    )
                },
            )
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
                    text = stringResource(R.string.choose_action_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                when (state) {
                    State.Loading -> LoadingScreen(modifier = Modifier.fillMaxSize())

                    is State.Data -> {
                        if (state.data.isEmpty()) {
                            EmptyScreen(
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            ListScreen(
                                modifier = Modifier.fillMaxSize(),
                                groups = state.data,
                                onClickAction = onClickAction,
                            )
                        }
                    }
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
private fun EmptyScreen(modifier: Modifier = Modifier) {
    Box(modifier) {
        val shrug = stringResource(R.string.shrug)
        val text = stringResource(R.string.action_list_empty)
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = buildAnnotatedString {
                withStyle(MaterialTheme.typography.headlineLarge.toSpanStyle()) {
                    append(shrug)
                }
                appendLine()
                appendLine()
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle()) {
                    append(text)
                }
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ListScreen(
    modifier: Modifier = Modifier,
    groups: List<SimpleListItemGroup>,
    onClickAction: (String) -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 200.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (group in groups) {
            stickyHeader(contentType = "header") {
                SimpleListItemHeader(modifier = Modifier.fillMaxWidth(), text = group.header)
            }

            items(
                group.items,
                contentType = { "list_item" },
            ) { model ->
                SimpleListItemFixedHeight(
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
            query = "Search query",
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

                            SimpleListItemModel(
                                "long",
                                title = "Very very very very very very very long title",
                                icon = ComposeIconInfo.Vector(Icons.Rounded.Bluetooth),
                                subtitle = null,
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
