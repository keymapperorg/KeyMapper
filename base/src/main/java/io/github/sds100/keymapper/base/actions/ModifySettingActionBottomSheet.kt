package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.compose.BottomSheet
import io.github.sds100.keymapper.base.utils.ui.compose.BottomSheetDefaults
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperDropdownMenu
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
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    delegate.modifySettingActionBottomSheetState = null
                }
            },
            sheetState = sheetState,
        ) {
            ModifySettingActionBottomSheetContent(
                state = delegate.modifySettingActionBottomSheetState!!,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        delegate.modifySettingActionBottomSheetState = null
                    }
                },
                onChooseSetting = { settingType ->
                    delegate.onChooseSettingClick(settingType)
                },
                onComplete = { action ->
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        delegate.onDoneModifySettingClick(action)
                    }
                },
            )
        }
    }
}

@Composable
private fun ModifySettingActionBottomSheetContent(
    state: ModifySettingActionBottomSheetState,
    onDismiss: () -> Unit,
    onChooseSetting: (SettingType) -> Unit,
    onComplete: (ActionData.ModifySetting) -> Unit,
) {
    var settingType by remember(state) { mutableStateOf(state.settingType) }
    var settingKey by remember(state) { mutableStateOf(state.settingKey) }
    var value by remember(state) { mutableStateOf(state.value) }
    
    var settingTypeExpanded by remember { mutableStateOf(false) }

    BottomSheet(
        title = stringResource(R.string.action_modify_setting),
        onDismiss = onDismiss,
        positiveButton = BottomSheetDefaults.OkButton {
            val action = ActionData.ModifySetting(
                settingType = settingType,
                settingKey = settingKey,
                value = value,
            )
            onComplete(action)
        },
        positiveButtonEnabled = settingKey.isNotBlank() && value.isNotBlank(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Setting Type Dropdown
            KeyMapperDropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                expanded = settingTypeExpanded,
                onExpandedChange = { settingTypeExpanded = it },
                label = { Text(stringResource(R.string.modify_setting_type_label)) },
                selectedValue = settingType,
                values = listOf(
                    SettingType.SYSTEM to stringResource(R.string.modify_setting_type_system),
                    SettingType.SECURE to stringResource(R.string.modify_setting_type_secure),
                    SettingType.GLOBAL to stringResource(R.string.modify_setting_type_global),
                ),
                onValueChanged = { settingType = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Button to choose an existing setting
            Button(
                onClick = { onChooseSetting(settingType) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.choose_existing_setting))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Setting Key - manual entry
            OutlinedTextField(
                value = settingKey,
                onValueChange = { settingKey = it },
                label = { Text(stringResource(R.string.modify_setting_key_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.modify_setting_value_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            val exampleText = when (settingType) {
                SettingType.SYSTEM ->
                    stringResource(R.string.modify_setting_example_system)
                SettingType.SECURE ->
                    stringResource(R.string.modify_setting_example_secure)
                SettingType.GLOBAL ->
                    stringResource(R.string.modify_setting_example_global)
            }

            Text(
                text = exampleText,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
