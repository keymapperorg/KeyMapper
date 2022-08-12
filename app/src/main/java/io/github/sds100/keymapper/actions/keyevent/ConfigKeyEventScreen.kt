package io.github.sds100.keymapper.actions.keyevent

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.destinations.ChooseKeyCodeScreenDestination
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.ui.CheckBoxWithText
import io.github.sds100.keymapper.util.ui.ErrorOutlinedTextField

private const val contentPadding: Int = 16

@Destination
@Composable
fun ConfigKeyEventScreen(
    viewModel: ConfigKeyEventViewModel,
    resultBackNavigator: ResultBackNavigator<ActionData.InputKeyEvent>,
    navigator: DestinationsNavigator,
    keyCodeResultRecipient: ResultRecipient<ChooseKeyCodeScreenDestination, Int>
) {
    keyCodeResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
            }

            is NavResult.Value -> {
                viewModel.onChooseKeyCode(result.value)
            }
        }
    }

    val state by viewModel.state.collectAsState()

    ConfigKeyEventScreen(
        state = state,
        onDoneClick = {
            viewModel.createResult()?.also { resultBackNavigator.navigateBack(it) }
        },
        onBackClick = resultBackNavigator::navigateBack,
        onChooseKeyCodeClick = {
            navigator.navigate(ChooseKeyCodeScreenDestination)
        },
        onKeyCodeTextChange = viewModel::onKeyCodeTextChange,
        onSelectNoDevice = viewModel::onSelectNoDevice,
        onSelectDevice = viewModel::onSelectDevice,
        onModifierCheckedChange = { modifier, checked ->
            if (checked) {
                viewModel.onSelectModifier(modifier)
            } else {
                viewModel.onDeselectModifier(modifier)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigKeyEventScreen(
    state: ConfigKeyEventState,
    onDoneClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onChooseKeyCodeClick: () -> Unit = {},
    onKeyCodeTextChange: (String) -> Unit = {},
    onSelectNoDevice: () -> Unit = {},
    onSelectDevice: (String) -> Unit = {},
    onModifierCheckedChange: (Int, Boolean) -> Unit = { _, _ -> }
) {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    AnimatedVisibility(state.isDoneButtonEnabled, enter = fadeIn(), exit = fadeOut()) {
                        FloatingActionButton(
                            onClick = onDoneClick,
                            elevation = BottomAppBarDefaults.BottomAppBarFabElevation
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = stringResource(R.string.config_key_event_screen_done_content_description)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.choose_action_back_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(contentPadding.dp))

            KeyCodeSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = contentPadding.dp, end = contentPadding.dp),
                keyCodeText = state.keyCode,
                keyCodeName = state.keyCodeName,
                error = state.keyCodeError,
                onChooseClick = onChooseKeyCodeClick,
                onTextChange = onKeyCodeTextChange
            )

            Spacer(Modifier.height(16.dp))
            Divider(Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            DeviceSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = contentPadding.dp, end = contentPadding.dp),
                selectedDevice = state.selectedDevice,
                devices = state.devices,
                onSelectNoDevice = onSelectNoDevice,
                onDeviceSelect = onSelectDevice,
            )

            Spacer(Modifier.height(16.dp))
            Divider(Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            ModifierSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = contentPadding.dp, end = contentPadding.dp),
                selectedModifiers = state.selectedModifiers,
                onCheckedChange = onModifierCheckedChange
            )

            Spacer(Modifier.height(contentPadding.dp))
        }
    }
}

@Composable
private fun KeyCodeSection(
    modifier: Modifier = Modifier,
    keyCodeText: String,
    keyCodeName: String,
    error: KeyCodeError,
    onChooseClick: () -> Unit,
    onTextChange: (String) -> Unit
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.config_key_event_screen_key_code_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = keyCodeName,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(8.dp))

        val focusManager = LocalFocusManager.current

        val errorMessage: String = when (error) {
            KeyCodeError.NONE -> ""
            KeyCodeError.EMPTY -> stringResource(R.string.create_key_event_screen_empty_error)
            KeyCodeError.NOT_NUMBER -> stringResource(R.string.create_key_event_screen_not_an_integer_error)
        }

        ErrorOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            text = keyCodeText,
            errorMessage = errorMessage,
            isError = error != KeyCodeError.NONE,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChange = onTextChange,
            keyboardActions = KeyboardActions {
                focusManager.clearFocus()
            }
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = onChooseClick) {
            Text(stringResource(R.string.config_key_event_screen_choose_key_code_button))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSection(
    modifier: Modifier = Modifier,
    selectedDevice: DeviceItem?,
    devices: List<DeviceItem>,
    onSelectNoDevice: () -> Unit,
    onDeviceSelect: (String) -> Unit,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.config_key_event_screen_devices_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.config_key_event_screen_devices_explanation),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }
        val noDeviceName = stringResource(R.string.create_key_event_screen_no_device)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            }) {
            val focusManager = LocalFocusManager.current

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = selectedDevice?.name ?: noDeviceName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(expanded = expanded, onDismissRequest = {
                expanded = false
                focusManager.clearFocus()
            }) {
                DropdownMenuItem(
                    text = {
                        Text(noDeviceName)
                    },
                    onClick = {
                        onSelectNoDevice()
                        expanded = false
                        focusManager.clearFocus()
                    }
                )

                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.name) },
                        onClick = {
                            onDeviceSelect(device.id)
                            expanded = false
                            focusManager.clearFocus()
                        })
                }
            }
        }
    }
}

@Composable
private fun ModifierSection(
    modifier: Modifier = Modifier,
    selectedModifiers: Set<Int>,
    onCheckedChange: (Int, Boolean) -> Unit
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.config_key_event_screen_modifiers_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(8.dp))

        val minWidthDp = 150.dp
        val rowWidthDp = LocalConfiguration.current.screenWidthDp.dp - ((contentPadding * 2).dp)
        val rowCount: Int = (rowWidthDp / minWidthDp).toInt()
        val itemWidthDp = (rowWidthDp / rowCount)

        FlowRow(Modifier.fillMaxWidth()) {
            KeyEventUtils.MODIFIER_LABELS.forEach { (modifier, label) ->
                CheckBoxWithText(
                    modifier = Modifier.width(itemWidthDp),
                    isChecked = selectedModifiers.contains(modifier),
                    text = stringResource(label),
                    onCheckedChange = { onCheckedChange(modifier, it) }
                )
            }
        }
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun ConfigKeyEventScreenPreview() {
    MaterialTheme {
        Surface {
            ConfigKeyEventScreen(
                state = ConfigKeyEventState(
                    keyCode = "123",
                    keyCodeName = "KEYCODE_A",
                    keyCodeError = KeyCodeError.NOT_NUMBER,
                    selectedModifiers = setOf(KeyEvent.KEYCODE_CTRL_LEFT),
                    devices = listOf(DeviceItem("0", "No Device"), DeviceItem("1", "Keyboard")),
                    selectedDevice = DeviceItem("1", "Keyboard"),
                    isDoneButtonEnabled = true
                )
            )
        }
    }
}