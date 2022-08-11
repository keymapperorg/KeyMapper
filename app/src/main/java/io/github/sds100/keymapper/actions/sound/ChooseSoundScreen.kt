package io.github.sds100.keymapper.actions.sound

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.ui.TextFieldDialog

@Destination
@Composable
fun ChooseSoundScreen(
    viewModel: ChooseSoundFileViewModel2,
    resultBackNavigator: ResultBackNavigator<ChooseSoundResult>
) {
    val listItems by viewModel.listItems.collectAsState()

    val chooseFileLauncher: ManagedActivityResultLauncher<String, Uri?> =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            viewModel.onChooseNewSoundFile(uri.toString())
        }

    val configState = viewModel.configState

    if (configState is ConfigSoundActionState.Finished) {
        resultBackNavigator.navigateBack(configState.result)
    }

    ChooseSoundScreen(
        listItems = listItems,
        configState = configState,
        onBackClick = resultBackNavigator::navigateBack,
        onChooseNewSoundClick = {
            chooseFileLauncher.launch(FileUtils.MIME_TYPE_AUDIO)
        },
        onListItemClick = viewModel::onListItemClick,
        onCreateSoundName = viewModel::onCreateNewSoundFileName,
        onDismissCreatingSoundName = viewModel::onDismissCreatingSoundFileName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseSoundScreen(
    modifier: Modifier = Modifier,
    listItems: List<ChooseSoundListItem>,
    configState: ConfigSoundActionState,
    onBackClick: () -> Unit = {},
    onChooseNewSoundClick: () -> Unit = {},
    onListItemClick: (String) -> Unit = {},
    onCreateSoundName: (String) -> Unit = {},
    onDismissCreatingSoundName: () -> Unit = {}
) {
    Scaffold(
        modifier,
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.choose_action_back_content_description)
                    )
                }
            }
        }) { padding ->
        Content(
            modifier = Modifier.padding(padding),
            listItems = listItems,
            configState = configState,
            onChooseNewSoundClick = onChooseNewSoundClick,
            onListItemClick = onListItemClick,
            onCreateSoundName = onCreateSoundName,
            onDismissCreatingSoundName = onDismissCreatingSoundName
        )
    }
}

@Preview
@Composable
private fun Preview() {
    MaterialTheme {
        ChooseSoundScreen(
            listItems = listOf(
                ChooseSoundListItem("1", "Sound 1"),
                ChooseSoundListItem("2", "Sound 2")
            ),
            configState = ConfigSoundActionState.Idle
        )
    }
}

@Composable
private fun Content(
    modifier: Modifier,
    listItems: List<ChooseSoundListItem>,
    configState: ConfigSoundActionState,
    onChooseNewSoundClick: () -> Unit,
    onListItemClick: (String) -> Unit,
    onCreateSoundName: (String) -> Unit,
    onDismissCreatingSoundName: () -> Unit
) {
    if (configState is ConfigSoundActionState.CreateSoundName) {
        var text by rememberSaveable { mutableStateOf(configState.fileName) }
        val emptyErrorString = stringResource(R.string.choose_action_url_empty_error)
        val error by derivedStateOf {
            when {
                text.isEmpty() -> emptyErrorString
                else -> null
            }
        }

        TextFieldDialog(
            text = text,
            title = stringResource(R.string.choose_sound_file_description_title),
            label = stringResource(R.string.choose_sound_file_description_label),
            error = error,
            onTextChange = { text = it },
            onConfirm = onCreateSoundName,
            onDismiss = onDismissCreatingSoundName
        )
    }

    Column(modifier.padding(8.dp)) {
        Text(stringResource(R.string.choose_sound_file_caption))

        Spacer(Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onChooseNewSoundClick
        ) {
            Text(stringResource(R.string.choose_sound_file_button))
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.fillMaxWidth()) {
            items(listItems) { listItem ->
                SimpleListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = listItem.title,
                    onClick = { onListItemClick(listItem.uid) }
                )
            }
        }
    }
}
