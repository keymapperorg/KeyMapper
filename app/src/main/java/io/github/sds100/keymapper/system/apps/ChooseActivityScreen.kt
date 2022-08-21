package io.github.sds100.keymapper.system.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.KMIcon
import io.github.sds100.keymapper.util.ui.SearchAppBar
import io.github.sds100.keymapper.util.ui.SearchState
import io.github.sds100.keymapper.util.ui.SimpleListItem

@Destination
@Composable
fun ChooseActivityScreen(
    viewModel: ChooseActivityViewModel2,
    resultBackNavigator: ResultBackNavigator<ActivityInfo>
) {
    val searchState by viewModel.searchState.collectAsState()

    ChooseActivityScreen(
        state = viewModel.state,
        searchState = searchState,
        setSearchState = viewModel::setSearchState,
        onBackClick = resultBackNavigator::navigateBack,
        onActivityClick = { listItem ->
            val activityInfo = ActivityInfo(listItem.activityName, listItem.packageName)
            resultBackNavigator.navigateBack(activityInfo)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseActivityScreen(
    modifier: Modifier = Modifier,
    state: ChooseActivityState,
    searchState: SearchState,
    setSearchState: (SearchState) -> Unit = {},
    onBackClick: () -> Unit = {},
    onActivityClick: (ChooseActivityListItem) -> Unit = {},
) {
    Scaffold(modifier, bottomBar = {
        SearchAppBar(onBackClick, searchState, setSearchState) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.choose_action_back_content_description)
                )
            }
        }
    }) { padding ->
        when (state) {
            is ChooseActivityState.Loaded -> LoadedUi(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                listItems = state.listItems,
                onActivityClick = onActivityClick
            )

            ChooseActivityState.Loading -> LoadingUi(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun LoadingUi(modifier: Modifier = Modifier) {
    Box(modifier) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun LoadedUi(
    modifier: Modifier = Modifier,
    listItems: List<ChooseActivityListItem>,
    onActivityClick: (ChooseActivityListItem) -> Unit
) {
    ChooseActivityList(
        modifier = modifier,
        listItems = listItems,
        onActivityClick = onActivityClick
    )
}

@Composable
private fun ChooseActivityList(
    modifier: Modifier = Modifier,
    listItems: List<ChooseActivityListItem>,
    onActivityClick: (ChooseActivityListItem) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            listItems,
            key = { listItem -> listItem.packageName + listItem.activityName }
        ) { listItem ->
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                title = listItem.activityName,
                subtitle = listItem.packageName,
                icon = listItem.icon,
                onClick = { onActivityClick(listItem) }
            )
        }
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun LoadedPreview() {
    MaterialTheme {
        val state = ChooseActivityState.Loaded(
            listItems = listOf(
                ChooseActivityListItem(
                    packageName = "packagename",
                    activityName = "Key Mapper",
                    icon = KMIcon.ImageVector(Icons.Outlined.Android))
            )
        )

        ChooseActivityScreen(
            state = state,
            searchState = SearchState.Idle
        )
    }
}