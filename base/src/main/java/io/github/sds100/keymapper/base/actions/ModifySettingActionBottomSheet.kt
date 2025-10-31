package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import kotlinx.coroutines.launch

data class ModifySettingActionBottomSheetState(
    val settingType: io.github.sds100.keymapper.system.settings.SettingType,
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
    onComplete: (ActionData.ModifySetting) -> Unit,
) {
    var settingType by remember(state) { mutableStateOf(state.settingType) }
    var settingKey by remember(state) { mutableStateOf(state.settingKey) }
    var value by remember(state) { mutableStateOf(state.value) }
    
    var settingTypeExpanded by remember { mutableStateOf(false) }
    var settingKeyExpanded by remember { mutableStateOf(false) }

    // Available setting keys based on selected type - placeholder, would need SettingsAdapter
    val availableKeys = remember(settingType) {
        // For now, return empty list. This will be populated via dependency injection
        emptyList<String>()
    }

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
            ExposedDropdownMenuBox(
                expanded = settingTypeExpanded,
                onExpandedChange = { settingTypeExpanded = it },
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = settingTypeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = settingTypeExpanded,
                    onDismissRequest = { settingTypeExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.modify_setting_type_system)) },
                        onClick = {
                            settingType = io.github.sds100.keymapper.system.settings.SettingType.SYSTEM
                            settingTypeExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.modify_setting_type_secure)) },
                        onClick = {
                            settingType = io.github.sds100.keymapper.system.settings.SettingType.SECURE
                            settingTypeExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.modify_setting_type_global)) },
                        onClick = {
                            settingType = io.github.sds100.keymapper.system.settings.SettingType.GLOBAL
                            settingTypeExpanded = false
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Setting Key - allow both dropdown selection and manual entry
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
                io.github.sds100.keymapper.system.settings.SettingType.SYSTEM ->
                    stringResource(R.string.modify_setting_example_system)
                io.github.sds100.keymapper.system.settings.SettingType.SECURE ->
                    stringResource(R.string.modify_setting_example_secure)
                io.github.sds100.keymapper.system.settings.SettingType.GLOBAL ->
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
