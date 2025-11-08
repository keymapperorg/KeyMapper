package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.base.utils.ui.compose.SearchAppBarActions
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.settings.SettingType
import kotlinx.coroutines.flow.update

@Composable
fun ChooseSettingScreen(modifier: Modifier = Modifier, viewModel: ChooseSettingViewModel) {
    val state by viewModel.settings.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settingType by viewModel.selectedSettingType.collectAsStateWithLifecycle()

    ChooseSettingScreen(
        modifier = modifier,
        state = state,
        query = query,
        settingType = settingType,
        onQueryChange = { newQuery -> viewModel.searchQuery.update { newQuery } },
        onCloseSearch = { viewModel.searchQuery.update { null } },
        onSettingTypeChange = { viewModel.selectedSettingType.value = it },
        onClickSetting = viewModel::onSettingClick,
        onNavigateBack = viewModel::onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseSettingScreen(
    modifier: Modifier = Modifier,
    state: State<List<SettingItem>>,
    query: String? = null,
    settingType: SettingType,
    onQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onSettingTypeChange: (SettingType) -> Unit = {},
    onClickSetting: (String, String?) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_setting_title)) },
            )
        },
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                KeyMapperSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    buttonStates = listOf(
                        SettingType.SYSTEM to stringResource(R.string.modify_setting_type_system),
                        SettingType.SECURE to stringResource(R.string.modify_setting_type_secure),
                        SettingType.GLOBAL to stringResource(R.string.modify_setting_type_global),
                    ),
                    selectedState = settingType,
                    onStateSelected = onSettingTypeChange,
                )

                HorizontalDivider()

                when (state) {
                    State.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is State.Data -> {
                        if (state.data.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.choose_setting_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.data) { item ->
                                    ListItem(
                                        headlineContent = { Text(item.key) },
                                        supportingContent = item.value?.let { { Text(it) } },
                                        modifier = Modifier.clickable {
                                            onClickSetting(item.key, item.value)
                                        },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewWithData() {
    KeyMapperTheme {
        ChooseSettingScreen(
            state = State.Data(
                listOf(
                    SettingItem("adb_enabled", "0"),
                    SettingItem("airplane_mode_on", "0"),
                    SettingItem("bluetooth_on", "1"),
                    SettingItem("screen_brightness", "128"),
                    SettingItem("wifi_on", "1"),
                ),
            ),
            settingType = SettingType.GLOBAL,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewLoading() {
    KeyMapperTheme {
        ChooseSettingScreen(
            state = State.Loading,
            settingType = SettingType.SECURE,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        ChooseSettingScreen(
            state = State.Data(emptyList()),
            settingType = SettingType.SYSTEM,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewWithSearch() {
    KeyMapperTheme {
        ChooseSettingScreen(
            state = State.Data(
                listOf(
                    SettingItem("bluetooth_on", "1"),
                ),
            ),
            query = "bluetooth",
            settingType = SettingType.SECURE,
        )
    }
}
