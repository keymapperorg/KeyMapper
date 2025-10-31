package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.compose.SearchAppBarActions
import io.github.sds100.keymapper.common.utils.State
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
    settingType: io.github.sds100.keymapper.system.settings.SettingType,
    onQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onSettingTypeChange: (io.github.sds100.keymapper.system.settings.SettingType) -> Unit = {},
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
                // Setting type dropdown
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    OutlinedTextField(
                        value = when (settingType) {
                            io.github.sds100.keymapper.system.settings.SettingType.SYSTEM ->
                                stringResource(R.string.modify_setting_type_system)
                            io.github.sds100.keymapper.system.settings.SettingType.SECURE ->
                                stringResource(R.string.modify_setting_type_secure)
                            io.github.sds100.keymapper.system.settings.SettingType.GLOBAL ->
                                stringResource(R.string.modify_setting_type_global)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.modify_setting_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.modify_setting_type_system)) },
                            onClick = {
                                onSettingTypeChange(io.github.sds100.keymapper.system.settings.SettingType.SYSTEM)
                                expanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.modify_setting_type_secure)) },
                            onClick = {
                                onSettingTypeChange(io.github.sds100.keymapper.system.settings.SettingType.SECURE)
                                expanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.modify_setting_type_global)) },
                            onClick = {
                                onSettingTypeChange(io.github.sds100.keymapper.system.settings.SettingType.GLOBAL)
                                expanded = false
                            },
                        )
                    }
                }

                HorizontalDivider()

                // Settings list
                when (state) {
                    State.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is State.Data -> {
                        if (state.data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
