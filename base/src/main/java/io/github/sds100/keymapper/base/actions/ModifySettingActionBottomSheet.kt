package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import kotlinx.coroutines.launch

sealed class ModifySettingActionBottomSheetState(
    open val settingKey: String,
    open val value: String,
) {
    data class System(
        override val settingKey: String,
        override val value: String,
    ) : ModifySettingActionBottomSheetState(settingKey, value)

    data class Secure(
        override val settingKey: String,
        override val value: String,
    ) : ModifySettingActionBottomSheetState(settingKey, value)

    data class Global(
        override val settingKey: String,
        override val value: String,
    ) : ModifySettingActionBottomSheetState(settingKey, value)
}

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
    var settingKey by remember(state) { mutableStateOf(state.settingKey) }
    var value by remember(state) { mutableStateOf(state.value) }

    val titleRes = when (state) {
        is ModifySettingActionBottomSheetState.System -> R.string.action_modify_system_setting
        is ModifySettingActionBottomSheetState.Secure -> R.string.action_modify_secure_setting
        is ModifySettingActionBottomSheetState.Global -> R.string.action_modify_global_setting
    }

    BottomSheet(
        title = stringResource(titleRes),
        onDismiss = onDismiss,
        positiveButton = BottomSheetDefaults.OkButton {
            val action = when (state) {
                is ModifySettingActionBottomSheetState.System ->
                    ActionData.ModifySetting.System(settingKey, value)
                is ModifySettingActionBottomSheetState.Secure ->
                    ActionData.ModifySetting.Secure(settingKey, value)
                is ModifySettingActionBottomSheetState.Global ->
                    ActionData.ModifySetting.Global(settingKey, value)
            }
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

            val exampleText = when (state) {
                is ModifySettingActionBottomSheetState.System ->
                    stringResource(R.string.modify_setting_example_system)
                is ModifySettingActionBottomSheetState.Secure ->
                    stringResource(R.string.modify_setting_example_secure)
                is ModifySettingActionBottomSheetState.Global ->
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
