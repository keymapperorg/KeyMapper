package io.github.sds100.keymapper.actions.keyevent

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.SearchAppBar
import io.github.sds100.keymapper.util.ui.SearchState
import io.github.sds100.keymapper.util.ui.SimpleListItem

@Destination
@Composable
fun ChooseKeyCodeScreen(
    viewModel: ChooseKeyCodeViewModel2,
    resultNavigator: ResultBackNavigator<Int>,
) {
    val searchState by viewModel.searchState.collectAsState()

    ChooseKeyCodeScreen(
        listItems = viewModel.listItems,
        searchState = searchState,
        setSearchState = viewModel::setSearchState,
        onKeyCodeClick = resultNavigator::navigateBack,
        onBackClick = resultNavigator::navigateBack
    )
}

/**
 * Created by sds100 on 28/07/2022.
 */

@Preview(device = Devices.PIXEL_3)
@Composable
private fun ChooseKeyCodeScreenPreview() {
    MaterialTheme {
        ChooseKeyCodeScreen(
            listItems = listOf(
                KeyCodeListItem(1, "KEYCODE_VOLUME_DOWN"),
                KeyCodeListItem(2, "KEYCODE_VOLUME_UP"),
            ),
            searchState = SearchState.Idle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseKeyCodeScreen(
    modifier: Modifier = Modifier,
    listItems: List<KeyCodeListItem>,
    searchState: SearchState,
    setSearchState: (SearchState) -> Unit = {},
    onKeyCodeClick: (Int) -> Unit = {},
    onBackClick: () -> Unit = {}
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
        ChooseKeyCodeList(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            listItems = listItems, onKeyCodeClick = onKeyCodeClick
        )
    }
}

@Composable
private fun ChooseKeyCodeList(
    modifier: Modifier = Modifier,
    listItems: List<KeyCodeListItem>,
    onKeyCodeClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        state = listState
    ) {
        items(
            listItems,
            key = { listItem -> listItem.keyCode }
        ) { listItem ->
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                title = listItem.label,
                onClick = { onKeyCodeClick(listItem.keyCode) }
            )
        }
    }
}