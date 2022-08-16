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
import io.github.sds100.keymapper.util.ui.*

/**
 * Created by sds100 on 30/07/2022.
 */

@Destination
@Composable
fun ChooseAppScreen(
    viewModel: ChooseAppViewModel2,
    allowHiddenApps: Boolean = true,
    resultBackNavigator: ResultBackNavigator<String>
) {
    val searchState by viewModel.searchState.collectAsState()

    ChooseAppScreen(
        state = viewModel.state,
        searchState = searchState,
        setSearchState = viewModel::setSearchState,
        onBackClick = resultBackNavigator::navigateBack,
        onAppClick = resultBackNavigator::navigateBack,
        onHiddenAppsCheckedChange = viewModel::onHiddenAppsCheckedChange,
        showHiddenAppsCheckBox = allowHiddenApps
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseAppScreen(
    modifier: Modifier = Modifier,
    state: ChooseAppState,
    searchState: SearchState,
    setSearchState: (SearchState) -> Unit = {},
    onBackClick: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
    showHiddenAppsCheckBox: Boolean = true,
    onHiddenAppsCheckedChange: (Boolean) -> Unit = {}
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
            is ChooseAppState.Loaded -> LoadedUi(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                listItems = state.listItems,
                showHiddenAppsCheckBox = showHiddenAppsCheckBox,
                isHiddenAppsChecked = state.isHiddenAppsChecked,
                onHiddenAppsCheckedChange = onHiddenAppsCheckedChange,
                onAppClick = onAppClick
            )

            ChooseAppState.Loading -> LoadingUi(
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
    listItems: List<ChooseAppListItem>,
    isHiddenAppsChecked: Boolean,
    showHiddenAppsCheckBox: Boolean = true,
    onHiddenAppsCheckedChange: (Boolean) -> Unit,
    onAppClick: (String) -> Unit
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ChooseAppList(
            modifier = Modifier.weight(1f),
            listItems = listItems,
            onAppClick = onAppClick
        )

        if (showHiddenAppsCheckBox) {
            CheckBoxWithText(
                modifier = Modifier.fillMaxWidth(),
                isChecked = isHiddenAppsChecked,
                text = { Text(stringResource(R.string.show_hidden_apps)) },
                onCheckedChange = onHiddenAppsCheckedChange
            )
        }
    }
}

@Composable
private fun ChooseAppList(
    modifier: Modifier = Modifier,
    listItems: List<ChooseAppListItem>,
    onAppClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        state = listState
    ) {
        items(
            listItems,
            key = { listItem -> listItem.packageName }
        ) { listItem ->
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                title = listItem.name,
                icon = listItem.icon,
                onClick = { onAppClick(listItem.packageName) }
            )
        }
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun LoadingPreview() {
    MaterialTheme {
        ChooseAppScreen(
            state = ChooseAppState.Loading,
            searchState = SearchState.Idle
        )
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun LoadedPreview() {
    MaterialTheme {
        val state = ChooseAppState.Loaded(
            listItems = listOf(
                ChooseAppListItem("packagename1", "Key Mapper", KMIcon.ImageVector(Icons.Outlined.Android))
            ),
            isHiddenAppsChecked = true
        )

        ChooseAppScreen(
            state = state,
            searchState = SearchState.Idle
        )
    }
} 