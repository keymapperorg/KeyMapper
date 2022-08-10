package io.github.sds100.keymapper.system.apps

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 30/07/2022.
 */

@Suppress("DEPRECATION")
@Destination
@Composable
fun ChooseAppShortcutScreen(
    viewModel: ChooseAppShortcutViewModel2,
    resultBackNavigator: ResultBackNavigator<ChooseAppShortcutResult>
) {
    val searchState by viewModel.searchState.collectAsState()

    val configAppShortcutLauncher: ManagedActivityResultLauncher<Intent, ActivityResult> =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val intent = result.data!!
                viewModel.onConfigShortcutResult(intent)
            }
        }

    val configState = viewModel.configState

    if (configState is ConfigAppShortcutState.Finished) {
        resultBackNavigator.navigateBack(configState.result)
    }

    ChooseAppShortcutScreen(
        chooseState = viewModel.chooseState,
        configState = viewModel.configState,
        searchState = searchState,
        setSearchState = viewModel::setSearchState,
        onBackClick = resultBackNavigator::navigateBack,
        launchShortcutConfig = { shortcut ->
            val intent = buildConfigShortcutIntent(shortcut)
            configAppShortcutLauncher.launch(intent)
        },
        onChooseName = viewModel::onCreateShortcutName,
        onDismissChoosingName = viewModel::onDismissCreatingShortcutName
    )
}

private fun buildConfigShortcutIntent(shortcutInfo: AppShortcutInfo): Intent {
    val intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
    intent.setClassName(shortcutInfo.packageName, shortcutInfo.activityName)
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 1)

    return intent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseAppShortcutScreen(
    modifier: Modifier = Modifier,
    chooseState: ChooseAppShortcutState,
    configState: ConfigAppShortcutState,
    searchState: SearchState,
    setSearchState: (SearchState) -> Unit = {},
    onBackClick: () -> Unit = {},
    launchShortcutConfig: (AppShortcutInfo) -> Unit = {},
    onChooseName: (String) -> Unit = {},
    onDismissChoosingName: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noPermissionMessage = stringResource(R.string.error_keymapper_doesnt_have_permission_app_shortcut)

    if (configState is ConfigAppShortcutState.ChooseName) {
        ChooseNameDialog(onConfirmClick = onChooseName, onDismissRequest = onDismissChoosingName)
    }

    Scaffold(
        modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SearchAppBar(onBackClick, searchState, setSearchState) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.choose_action_back_content_description)
                    )
                }
            }
        }) { padding ->
        when (chooseState) {
            is ChooseAppShortcutState.Loaded -> LoadedUi(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                listItems = chooseState.listItems,
                onShortcutClick = { shortcut ->
                    try {
                        launchShortcutConfig(shortcut)
                    } catch (e: SecurityException) {
                        scope.launch {
                            snackbarHostState.showSnackbar(noPermissionMessage)
                        }
                    }
                }
            )

            ChooseAppShortcutState.Loading -> LoadingUi(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun ChooseNameDialog(
    onConfirmClick: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var text: String by rememberSaveable { mutableStateOf("") }

    CustomDialog(
        title = stringResource(R.string.choose_app_shortcut_choose_name),
        confirmButton = {
            TextButton(onClick = { onConfirmClick(text) }) {
                Text(stringResource(R.string.pos_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismissRequest
    ) {
        TextField(value = text, onValueChange = { text = it })
    }
}

@Preview
@Composable
private fun ChooseNameDialogPreview() {
    ChooseNameDialog(onConfirmClick = {}, onDismissRequest = {})
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
    listItems: List<ChooseAppShortcutListItem>,
    onShortcutClick: (AppShortcutInfo) -> Unit = {}
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        state = listState
    ) {
        items(
            listItems,
            key = { listItem -> "${listItem.shortcutInfo.packageName}${listItem.shortcutInfo.activityName}" }
        ) { listItem ->
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                title = listItem.name,
                icon = listItem.icon,
                onClick = { onShortcutClick(listItem.shortcutInfo) }
            )
        }
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun LoadingPreview() {
    MaterialTheme {
        ChooseAppShortcutScreen(
            chooseState = ChooseAppShortcutState.Loading,
            searchState = SearchState.Idle,
            configState = ConfigAppShortcutState.Idle
        )
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun LoadedPreview() {
    MaterialTheme {
        val state = ChooseAppShortcutState.Loaded(
            listOf(
                ChooseAppShortcutListItem(
                    AppShortcutInfo("", ""),
                    "Key Mapper Shortcut",
                    KMIcon.ImageVector(Icons.Outlined.Android)
                )
            )
        )

        ChooseAppShortcutScreen(
            chooseState = state,
            searchState = SearchState.Idle,
            configState = ConfigAppShortcutState.Idle
        )
    }
} 