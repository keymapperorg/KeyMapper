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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow
import io.github.sds100.keymapper.system.settings.SettingType
import kotlinx.coroutines.launch

data class ModifySettingActionBottomSheetState(
    val settingType: SettingType,
    val settingKey: String,
    val value: String,
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
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

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
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )

            OutlinedTextField(
                value = state.value,
                onValueChange = onSettingValueChange,
                label = { Text(stringResource(R.string.modify_setting_value_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )

            // TODO do not allow empty text fields
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
                    onClick = onDoneClick,
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
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
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
