package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.LocalCustomColorsPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpadTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: ConfigTriggerViewModel,
    sheetState: SheetState,
) {
    val state by viewModel.dpadTriggerSetupState.collectAsStateWithLifecycle()

    DpadTriggerSetupBottomSheet(
        modifier,
        state = state,
        sheetState,
        onDismissRequest,
        onEnableKeyboardClick = viewModel::onEnableGuiKeyboardClick,
        onChooseKeyboardClick = viewModel::onChooseGuiKeyboardClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DpadTriggerSetupBottomSheet(
    modifier: Modifier = Modifier,
    state: DpadTriggerSetupState,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onEnableKeyboardClick: () -> Unit = {},
    onChooseKeyboardClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.dpad_trigger_setup_bottom_sheet_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.dpad_trigger_setup_bottom_sheet_text),
            )

            Spacer(modifier = Modifier.height(16.dp))

            val guiKeyboardUrl = stringResource(R.string.url_play_store_keymapper_gui_keyboard)
            StepRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                isEnabled = !state.isKeyboardInstalled,
                rowText = stringResource(R.string.dpad_trigger_setup_install_keyboard_text),
                buttonTextEnabled = stringResource(R.string.dpad_trigger_setup_install_keyboard_button),
                buttonTextDisabled = stringResource(R.string.dpad_trigger_setup_install_keyboard_button_disabled),
                onButtonClick = {
                    uriHandler.openUri(guiKeyboardUrl)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            StepRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                isEnabled = !state.isKeyboardEnabled,
                rowText = stringResource(R.string.dpad_trigger_setup_enable_keyboard_text),
                buttonTextEnabled = stringResource(R.string.dpad_trigger_setup_enable_keyboard_button),
                buttonTextDisabled = stringResource(R.string.dpad_trigger_setup_enable_keyboard_button_disabled),
                onButtonClick = onEnableKeyboardClick,
            )

            Spacer(modifier = Modifier.height(8.dp))

            StepRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                isEnabled = !state.isKeyboardChosen,
                rowText = stringResource(R.string.dpad_trigger_setup_choose_keyboard_text),
                buttonTextEnabled = stringResource(R.string.dpad_trigger_setup_choose_keyboard_button),
                buttonTextDisabled = stringResource(R.string.dpad_trigger_setup_choose_keyboard_button_disabled),
                onButtonClick = onChooseKeyboardClick,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isKeyboardInstalled && state.isKeyboardEnabled && state.isKeyboardChosen) {
                    Text(
                        text = "Setup complete!",
                        color = LocalCustomColorsPalette.current.green,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.pos_done))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepRow(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    rowText: String,
    buttonTextEnabled: String,
    buttonTextDisabled: String,
    onButtonClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = rowText,
            fontWeight = FontWeight.Medium,
        )

        FilledTonalButton(
            onClick = onButtonClick,
            enabled = isEnabled,
        ) {
            val text = if (isEnabled) {
                buttonTextEnabled
            } else {
                buttonTextDisabled
            }

            Text(text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = Expanded,
        )

        DpadTriggerSetupBottomSheet(
            onDismissRequest = {},
            state = DpadTriggerSetupState(
                isKeyboardInstalled = true,
                isKeyboardEnabled = false,
                isKeyboardChosen = false,
            ),
            sheetState = sheetState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewComplete() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = Expanded,
        )

        DpadTriggerSetupBottomSheet(
            onDismissRequest = {},
            state = DpadTriggerSetupState(
                isKeyboardInstalled = true,
                isKeyboardEnabled = true,
                isKeyboardChosen = true,
            ),
            sheetState = sheetState,
        )
    }
}
