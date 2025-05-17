package io.github.sds100.keymapper.base.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.util.drawable
import io.github.sds100.keymapper.base.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.util.ui.compose.SearchAppBarActions
import io.github.sds100.keymapper.base.util.ui.compose.SimpleListItem
import io.github.sds100.keymapper.base.util.ui.compose.SimpleListItemModel

@Composable
fun ChooseAppScreen(
    modifier: Modifier = Modifier,
    title: String,
    state: State<List<SimpleListItemModel>>,
    query: String? = null,
    onQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onClickApp: (String) -> Unit = {},
) {
    Scaffold(
        modifier.displayCutoutPadding(),
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
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (state) {
                    State.Loading -> LoadingScreen(modifier = Modifier.fillMaxSize())
                    is State.Data -> {
                        val items = state.data

                        if (items.isEmpty()) {
                            EmptyScreen(modifier = Modifier.fillMaxSize())
                        } else {
                            ListScreen(
                                modifier = Modifier.fillMaxSize(),
                                listItems = items,
                                onClick = onClickApp,
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
        val text = stringResource(R.string.app_list_empty)
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
    onClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(listItems) { model ->
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                model = model,
                onClick = { onClick(model.id) },
            )
        }
    }
}

@Preview
@Composable
private fun Empty() {
    KeyMapperTheme {
        ChooseAppScreen(title = "Choose app", state = State.Data(emptyList()))
    }
}

@Preview
@Composable
private fun Loading() {
    KeyMapperTheme {
        ChooseAppScreen(title = "Choose app", state = State.Loading)
    }
}

@Preview
@Composable
private fun Loaded() {
    val icon = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    KeyMapperTheme {
        ChooseAppScreen(
            title = "Choose app",
            state = State.Data(
                listOf(
                    SimpleListItemModel(
                        id = "1",
                        title = "Key Mapper",
                        icon = ComposeIconInfo.Drawable(icon),
                    ),
                    SimpleListItemModel(
                        id = "2",
                        title = "Key Mapper",
                        icon = ComposeIconInfo.Drawable(icon),
                    ),
                    SimpleListItemModel(
                        id = "3",
                        title = "Key Mapper",
                        icon = ComposeIconInfo.Drawable(icon),
                    ),
                ),
            ),
        )
    }
}
