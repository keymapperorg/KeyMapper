package io.github.sds100.keymapper.actions.keyevent

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.CheckBoxListItem
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 30/03/2020.
 */

class ConfigKeyEventViewModel(
    private val useCase: ConfigKeyEventUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider {

    private val state = MutableStateFlow(KeyEventState())

    private val _uiState = MutableStateFlow(
        buildUiState(
            state.value,
            inputDeviceList = emptyList(),
            showDeviceDescriptors = false
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _returnResult = MutableSharedFlow<ConfigKeyEventResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val rebuildUiState = MutableSharedFlow<Unit>()

    init {
        viewModelScope.launch {

            combine(
                state,
                useCase.inputDevices,
                useCase.showDeviceDescriptors
            ) { state, inputDevices, showDeviceDescriptors ->
                buildUiState(state, inputDevices, showDeviceDescriptors)
            }.collectLatest {
                _uiState.value = it
            }
        }
    }

    fun setModifierKeyChecked(modifier: Int, isChecked: Boolean) {
        val oldMetaState = state.value.metaState

        if (isChecked) {
            state.value = state.value.copy(
                metaState = oldMetaState.withFlag(modifier)
            )
        } else {
            state.value = state.value.copy(
                metaState = oldMetaState.minusFlag(modifier)
            )
        }
    }

    fun setKeyCode(keyCode: Int) {
        state.value = state.value.copy(keyCode = Success(keyCode))
    }

    fun onKeyCodeTextChanged(text: String) {
        val keyCodeState = when {
            text.isBlank() -> Error.EmptyText
            text.toIntOrNull() == null -> Error.InvalidNumber
            else -> text.toInt().success()
        }

        state.value = state.value.copy(keyCode = keyCodeState)
    }

    fun setUseShell(checked: Boolean) {
        state.value = state.value.copy(useShell = checked)
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun chooseNoDevice() {
        state.value = state.value.copy(chosenDevice = null)
    }

    fun chooseDevice(index: Int) {
        viewModelScope.launch {
            val chosenDevice = uiState.value.deviceListItems.getOrNull(index)

            if (chosenDevice == null) {
                return@launch
            }

            state.value = state.value.copy(
                chosenDevice = chosenDevice
            )
        }
    }

    fun onDoneClick() {
        viewModelScope.launch {
            val keyCode = state.value.keyCode.valueOrNull() ?: return@launch

            _returnResult.emit(
                ConfigKeyEventResult(
                    keyCode = keyCode,
                    metaState = state.value.metaState,
                    useShell = state.value.useShell,
                    device = state.value.chosenDevice
                )
            )
        }
    }

    fun refreshDevices() {
        rebuildUiState()
    }

    fun rebuildUiState() {
        runBlocking { rebuildUiState.emit(Unit) }
    }

    private fun buildUiState(
        state: KeyEventState,
        inputDeviceList: List<InputDeviceInfo>,
        showDeviceDescriptors: Boolean
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
            onError = { "" }
        )

        val modifierListItems = KeyEventUtils.MODIFIER_LABELS.map { (modifier, label) ->
            CheckBoxListItem(
                id = modifier.toString(),
                label = getString(label),
                isChecked = metaState.hasFlag(modifier)
            )
        }

        val deviceListItems = inputDeviceList.map { device ->
            if (showDeviceDescriptors) {
                device.copy(
                    name = InputDeviceUtils.appendDeviceDescriptorToName(
                        device.descriptor,
                        device.name
                    )
                )
            } else {
                device
            }
        }

        val chosenDeviceName: String = when {
            chosenDevice == null -> getString(R.string.from_no_device)
            showDeviceDescriptors -> InputDeviceUtils.appendDeviceDescriptorToName(
                chosenDevice.descriptor,
                chosenDevice.name
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
            chosenDeviceName = chosenDeviceName
        )
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: ConfigKeyEventUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ConfigKeyEventViewModel(useCase, resourceProvider) as T
        }
    }

    private data class KeyEventState(
        val keyCode: Result<Int> = Error.EmptyText,
        val chosenDevice: InputDeviceInfo? = null,
        val useShell: Boolean = false,
        val metaState: Int = 0
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
    val chosenDeviceName: String
)