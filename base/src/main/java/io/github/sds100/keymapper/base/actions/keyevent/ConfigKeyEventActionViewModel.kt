package io.github.sds100.keymapper.base.actions.keyevent

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.utils.InputEventStrings
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.CheckBoxListItem
import io.github.sds100.keymapper.base.utils.ui.NavDestination
import io.github.sds100.keymapper.base.utils.ui.NavigationViewModel
import io.github.sds100.keymapper.base.utils.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.navigate
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.errorOrNull
import io.github.sds100.keymapper.common.utils.handle
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.common.utils.minusFlag
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigKeyEventActionViewModel @Inject constructor(
    private val useCase: ConfigKeyEventUseCase,
    private val resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    NavigationViewModel by NavigationViewModelImpl() {

    private val keyEventState = MutableStateFlow(KeyEventState())

    val uiState: StateFlow<ConfigKeyEventUiState> = combine(
        keyEventState,
        useCase.inputDevices,
        useCase.showDeviceDescriptors,
    ) { state, inputDevices, showDeviceDescriptors ->
        buildUiState(state, inputDevices, showDeviceDescriptors)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        buildUiState(
            keyEventState.value,
            inputDeviceList = emptyList(),
            showDeviceDescriptors = false,
        ),
    )

    private val _returnResult = MutableSharedFlow<ActionData.InputKeyEvent>()
    val returnResult = _returnResult.asSharedFlow()

    fun setModifierKeyChecked(modifier: Int, isChecked: Boolean) {
        val oldMetaState = keyEventState.value.metaState

        if (isChecked) {
            keyEventState.value = keyEventState.value.copy(
                metaState = oldMetaState.withFlag(modifier),
            )
        } else {
            keyEventState.value = keyEventState.value.copy(
                metaState = oldMetaState.minusFlag(modifier),
            )
        }
    }

    fun onChooseKeyCodeClick() {
        viewModelScope.launch {
            val keyCode = navigate("choose_key_code_for_key_event", NavDestination.ChooseKeyCode)
                ?: return@launch

            setKeyCode(keyCode)
        }
    }

    fun setKeyCode(keyCode: Int) {
        keyEventState.value = keyEventState.value.copy(keyCode = Success(keyCode))
    }

    fun loadAction(action: ActionData.InputKeyEvent) {
        viewModelScope.launch {
            val inputDevice = useCase.inputDevices.first().find {
                it.descriptor == action.device?.descriptor &&
                    it.name == action.device.name
            }

            keyEventState.value = KeyEventState(
                Success(action.keyCode),
                inputDevice,
                useShell = action.useShell,
                metaState = action.metaState,
            )
        }
    }

    fun onKeyCodeTextChanged(text: String) {
        val keyCodeState = when {
            text.isBlank() -> Error.EmptyText
            text.toIntOrNull() == null -> Error.InvalidNumber
            else -> text.toInt().success()
        }

        keyEventState.value = keyEventState.value.copy(keyCode = keyCodeState)
    }

    fun setUseShell(checked: Boolean) {
        keyEventState.value = keyEventState.value.copy(useShell = checked)
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun chooseNoDevice() {
        keyEventState.value = keyEventState.value.copy(chosenDevice = null)
    }

    fun chooseDevice(index: Int) {
        val chosenDevice = uiState.value.deviceListItems.getOrNull(index)

        if (chosenDevice == null) {
            return
        }

        keyEventState.value = keyEventState.value.copy(
            chosenDevice = chosenDevice,
        )
    }

    fun onDoneClick() {
        viewModelScope.launch {
            val keyCode = keyEventState.value.keyCode.valueOrNull() ?: return@launch

            val device = keyEventState.value.chosenDevice?.let {
                ActionData.InputKeyEvent.Device(
                    descriptor = it.descriptor,
                    name = it.name,
                )
            }

            _returnResult.emit(
                ActionData.InputKeyEvent(
                    keyCode = keyCode,
                    metaState = keyEventState.value.metaState,
                    useShell = keyEventState.value.useShell,
                    device = device,
                ),
            )
        }
    }

    private fun buildUiState(
        state: KeyEventState,
        inputDeviceList: List<InputDeviceInfo>,
        showDeviceDescriptors: Boolean,
    ): ConfigKeyEventUiState {
        val keyCode = state.keyCode
        val metaState = state.metaState
        val useShell = state.useShell
        val chosenDevice = state.chosenDevice

        val keyCodeString = when (keyCode) {
            is Success -> keyCode.value.toString()
            else -> ""
        }

        val keyCodeLabel = keyCode.handle(
            onSuccess = {
                if (it > KeyEvent.getMaxKeyCode()) {
                    "Key Code $it"
                } else {
                    KeyEvent.keyCodeToString(it)
                }
            },
            onError = { "" },
        )

        val modifierListItems = InputEventStrings.MODIFIER_LABELS.map { (modifier, label) ->
            CheckBoxListItem(
                id = modifier.toString(),
                label = getString(label),
                isChecked = metaState.hasFlag(modifier),
            )
        }

        val deviceListItems = inputDeviceList.map { device ->
            if (showDeviceDescriptors) {
                device.copy(
                    name = InputDeviceUtils.appendDeviceDescriptorToName(
                        device.descriptor,
                        device.name,
                    ),
                )
            } else {
                device
            }
        }

        val chosenDeviceName: String = when {
            chosenDevice == null -> getString(R.string.from_no_device)
            showDeviceDescriptors -> InputDeviceUtils.appendDeviceDescriptorToName(
                chosenDevice.descriptor,
                chosenDevice.name,
            )

            else -> chosenDevice.name
        }

        return ConfigKeyEventUiState(
            keyCodeString = keyCodeString,
            keyCodeErrorMessage = keyCode.errorOrNull()?.getFullMessage(this),
            keyCodeLabel = keyCodeLabel,
            showKeyCodeLabel = keyCode.isSuccess,
            isUseShellChecked = useShell,
            isDevicePickerShown = !useShell,
            isModifierListShown = !useShell,
            modifierListItems = modifierListItems,
            isDoneButtonEnabled = keyCode.isSuccess,
            deviceListItems = deviceListItems,
            chosenDeviceName = chosenDeviceName,
        )
    }

    private data class KeyEventState(
        val keyCode: Result<Int> = Error.EmptyText,
        val chosenDevice: InputDeviceInfo? = null,
        val useShell: Boolean = false,
        val metaState: Int = 0,
    )
}

data class ConfigKeyEventUiState(
    val keyCodeString: String,
    val keyCodeErrorMessage: String?,
    val keyCodeLabel: String,
    val showKeyCodeLabel: Boolean,
    val isUseShellChecked: Boolean,
    val isDevicePickerShown: Boolean,
    val isModifierListShown: Boolean,
    val modifierListItems: List<CheckBoxListItem>,
    val isDoneButtonEnabled: Boolean,
    val deviceListItems: List<InputDeviceInfo>,
    val chosenDeviceName: String,
)
