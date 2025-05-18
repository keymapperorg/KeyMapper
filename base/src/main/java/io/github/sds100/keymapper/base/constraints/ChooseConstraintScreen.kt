package io.github.sds100.keymapper.base.constraints

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemFixedHeight
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemModel
import kotlinx.coroutines.flow.update

@Composable
fun ChooseConstraintScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseConstraintViewModel,
    onNavigateBack: () -> Unit,
) {
    val listItems by viewModel.listItems.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    TimeConstraintBottomSheet(viewModel)

    ChooseConstraintScreen(
        modifier = modifier,
        state = listItems,
        query = query,
        onQueryChange = { newQuery -> viewModel.searchQuery.update { newQuery } },
        onCloseSearch = { viewModel.searchQuery.update { null } },
        onClickAction = viewModel::onListItemClick,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseConstraintScreen(
    modifier: Modifier = Modifier,
    state: State<List<SimpleListItemModel>>,
    query: String? = null,
    onQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onClickAction: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    var isExpanded: Boolean by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.imePadding(),
                actions = {
                    IconButton(onClick = {
                        if (isExpanded) {
                            onCloseSearch()
                            isExpanded = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.bottom_app_bar_back_content_description),
                        )
                    }

                    DockedSearchBar(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        inputField = {
                            SearchBarDefaults.InputField(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                onSearch = {
                                    onQueryChange(it)
                                    isExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = null,
                                    )
                                },
                                enabled = state is State.Data,
                                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                query = query ?: "",
                                onQueryChange = onQueryChange,
                                expanded = isExpanded,
                                onExpandedChange = { expanded ->
                                    if (expanded) {
                                        isExpanded = true
                                    } else {
                                        onCloseSearch()
                                        isExpanded = false
                                    }
                                },
                            )
                        },
                        // This is false to prevent an empty "content" showing underneath.
                        expanded = isExpanded,
                        onExpandedChange = { expanded ->
                            if (expanded) {
                                isExpanded = true
                            } else {
                                onCloseSearch()
                                isExpanded = false
                            }
                        },
                        content = {},
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
                    text = stringResource(R.string.choose_constraint_title),
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
                                listItems = state.data,
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
    listItems: List<SimpleListItemModel>,
    onClickAction: (String) -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 200.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(listItems, key = { it.id }) { model ->
            SimpleListItemFixedHeight(
                modifier = Modifier.fillMaxWidth(),
                model = model,
                onClick = { onClickAction(model.id) },
            )
        }
    }
}

@Preview
@Composable
private fun PreviewList() {
    KeyMapperTheme {
        ChooseConstraintScreen(
            query = "Search query",
            state = State.Data(
                listOf(
                    SimpleListItemModel(
                        "app",
                        title = "App in foreground",
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                    ),
                    SimpleListItemModel(
                        "app",
                        title = "App not in foreground",
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                        subtitle = "Error",
                        isSubtitleError = true,
                        isEnabled = false,
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
        ChooseConstraintScreen(
            query = "Search query",
            state = State.Data(
                listOf(
                    SimpleListItemModel(
                        "app1",
                        title = "App in foreground",
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                    ),
                    SimpleListItemModel(
                        "app2",
                        title = "App not in foreground",
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Android),
                        subtitle = "Error",
                        isSubtitleError = true,
                        isEnabled = false,
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
        ChooseConstraintScreen(
            state = State.Loading,
        )
    }
}
