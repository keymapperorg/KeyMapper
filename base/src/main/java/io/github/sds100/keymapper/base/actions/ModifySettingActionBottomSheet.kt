package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.base.utils.ui.compose.filledTonalButtonColorsError
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.settings.SettingType
import kotlinx.coroutines.launch

data class ModifySettingActionBottomSheetState(
    val settingType: SettingType,
    val settingKey: String,
    val value: String,
    val testResult: KMResult<Unit>? = null,
    val isPermissionGranted: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifySettingActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.modifySettingActionBottomSheetState != null) {
        ModifySettingActionBottomSheet(
            sheetState = sheetState,
            state = delegate.modifySettingActionBottomSheetState!!,
            onDismissRequest = {
                delegate.modifySettingActionBottomSheetState = null
            },
            onSelectSettingType = delegate::onSelectSettingType,
            onSettingKeyChange = delegate::onSettingKeyChange,
            onSettingValueChange = delegate::onSettingValueChange,
            onChooseExistingClick = delegate::onChooseExistingSettingClick,
            onTestClick = delegate::onTestModifySettingClick,
            onRequestPermissionClick = delegate::onRequestModifySettingPermission,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneModifySettingClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModifySettingActionBottomSheet(
    sheetState: SheetState,
    state: ModifySettingActionBottomSheetState,
    onDismissRequest: () -> Unit = {},
    onSelectSettingType: (SettingType) -> Unit = {},
    onSettingKeyChange: (String) -> Unit = {},
    onSettingValueChange: (String) -> Unit = {},
    onChooseExistingClick: () -> Unit = {},
    onTestClick: () -> Unit = {},
    onRequestPermissionClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val settingKeyEmptyErrorString = stringResource(R.string.modify_setting_key_empty_error)
    val settingValueEmptyErrorString = stringResource(R.string.modify_setting_value_empty_error)

    var settingKeyError: String? by rememberSaveable { mutableStateOf(null) }
    var settingValueError: String? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect(state) {
        if (!state.settingKey.isBlank()){
            settingKeyError = null
        }

        if (!state.value.isBlank()){
            settingValueError = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.modify_setting_bottom_sheet_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            KeyMapperSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
                buttonStates = listOf(
                    SettingType.SYSTEM to stringResource(R.string.modify_setting_type_system),
                    SettingType.SECURE to stringResource(R.string.modify_setting_type_secure),
                    SettingType.GLOBAL to stringResource(R.string.modify_setting_type_global),
                ),
                selectedState = state.settingType,
                onStateSelected = onSelectSettingType,
            )

            if (!state.isPermissionGranted) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRequestPermissionClick,
                    colors = ButtonDefaults.filledTonalButtonColorsError(),
                ) {
                    Text(stringResource(R.string.modify_setting_grant_permission_button))
                }
            }

            Button(
                onClick = onChooseExistingClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.choose_existing_setting))
            }

            OutlinedTextField(
                value = state.settingKey,
                onValueChange = onSettingKeyChange,
                label = { Text(stringResource(R.string.modify_setting_key_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                isError = settingKeyError != null,
                supportingText = {
                    if (settingKeyError != null) {
                        Text(
                            text = settingKeyError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            OutlinedTextField(
                value = state.value,
                onValueChange = onSettingValueChange,
                label = { Text(stringResource(R.string.modify_setting_value_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                isError = settingValueError != null,
                supportingText = {
                    if (settingValueError != null) {
                        Text(
                            text = settingValueError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (state.testResult != null) {
                    val resultText: String = when (state.testResult) {
                        is Success -> stringResource(R.string.test_modify_setting_result_ok)
                        is KMError -> state.testResult.getFullMessage(LocalContext.current)
                    }

                    val textColor = when (state.testResult) {
                        is Success -> LocalCustomColorsPalette.current.green
                        is KMError -> MaterialTheme.colorScheme.error
                    }

                    Text(
                        modifier = Modifier.weight(1f),
                        text = resultText,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    onClick = {
                        var hasError = false

                        if (state.settingKey.isBlank()) {
                            settingKeyError = settingKeyEmptyErrorString
                            hasError = true
                        }

                        if (state.value.isBlank()) {
                            settingValueError = settingValueEmptyErrorString
                            hasError = true
                        }

                        if (!hasError) {
                            onTestClick()
                        }
                    },
                ) {
                    Text(stringResource(R.string.button_test_modify_setting))
                }
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.modify_setting_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.neg_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (state.settingKey.isBlank()) {
                            settingKeyError = settingKeyEmptyErrorString
                        }

                        if (state.value.isBlank()) {
                            settingValueError = settingValueEmptyErrorString
                        }

                        if (settingKeyError == null && settingValueError == null) {
                            onDoneClick()
                        }
                    },
                ) {
                    Text(stringResource(R.string.pos_done))
                }
            }
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
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        ModifySettingActionBottomSheet(
            sheetState = sheetState,
            state = ModifySettingActionBottomSheetState(
                settingType = SettingType.GLOBAL,
                settingKey = "adb_enabled",
                value = "1",
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        ModifySettingActionBottomSheet(
            sheetState = sheetState,
            state = ModifySettingActionBottomSheetState(
                settingType = SettingType.SYSTEM,
                settingKey = "",
                value = "",
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewPermissionNotGranted() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        ModifySettingActionBottomSheet(
            sheetState = sheetState,
            state = ModifySettingActionBottomSheetState(
                settingType = SettingType.SECURE,
                settingKey = "airplane_mode_on",
                value = "1",
                isPermissionGranted = false,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewTestLoading() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        ModifySettingActionBottomSheet(
            sheetState = sheetState,
            state = ModifySettingActionBottomSheetState(
                settingType = SettingType.GLOBAL,
                settingKey = "adb_enabled",
                value = "1",
                testResult = null,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewTestSuccess() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        ModifySettingActionBottomSheet(
            sheetState = sheetState,
            state = ModifySettingActionBottomSheetState(
                settingType = SettingType.GLOBAL,
                settingKey = "adb_enabled",
                value = "1",
                testResult = Success(Unit),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewTestError() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        ModifySettingActionBottomSheet(
            sheetState = sheetState,
            state = ModifySettingActionBottomSheetState(
                settingType = SettingType.SECURE,
                settingKey = "airplane_mode_on",
                value = "1",
                testResult = SystemError.PermissionDenied(Permission.WRITE_SECURE_SETTINGS),
            ),
        )
    }
}
