package io.github.sds100.keymapper.system.keyevents

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.devices.GetInputDevicesUseCase
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
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
    private val getInputDevices: GetInputDevicesUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider {

    private val keyCode = MutableStateFlow<Result<Int>>(Error.CantBeEmpty)
    private val chosenDevice = MutableStateFlow<InputDeviceInfo?>(null)
    private val useShell = MutableStateFlow(false)
    private val metaState = MutableStateFlow(0)

    private val _state = MutableStateFlow(
        buildUiState(
            keyCode = Error.CantBeEmpty,
            useShell = false,
            metaState = 0,
            chosenDevice = null,
            inputDeviceList = emptyList()
        )
    )
    val state = _state.asStateFlow()

    private val _returnResult = MutableSharedFlow<ConfigKeyEventResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val rebuildUiState = MutableSharedFlow<Unit>()

    init {
        viewModelScope.launch {

            combine(
                rebuildUiState,
                keyCode.combine(metaState) { keyCode, metaState -> Pair(keyCode, metaState) },
                useShell,
                chosenDevice,
                getInputDevices.devices
            ) { _, (keyCode, metaState), useShell, chosenDevice, devices ->
                buildUiState(
                    keyCode,
                    useShell,
                    metaState,
                    chosenDevice,
                    devices.dataOrNull() ?: emptyList()
                )
            }.collectLatest {
                _state.value = it
            }
        }
    }

    fun setModifierKeyChecked(modifier: Int, isChecked: Boolean) {
        if (isChecked) {
            metaState.value = metaState.value.withFlag(modifier)
        } else {
            metaState.value = metaState.value.minusFlag(modifier)
        }
    }

    fun setKeyCode(keyCode: Int) {
        this.keyCode.value = keyCode.success()
    }

    fun onKeyCodeTextChanged(text: String) {
        keyCode.value = when {
            text.isBlank() -> Error.CantBeEmpty
            text.toIntOrNull() == null -> Error.InvalidNumber
            else -> text.toInt().success()
        }
    }

    fun setUseShell(checked: Boolean) {
        useShell.value = checked
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun chooseNoDevice() {
        chosenDevice.value = null
    }

    fun chooseDevice(index: Int) {
        chosenDevice.value = state.value.deviceListItems.getOrNull(index)
    }

    fun onDoneClick() {
        viewModelScope.launch {
            val keyCode = keyCode.value.valueOrNull() ?: return@launch

            _returnResult.emit(
                ConfigKeyEventResult(
                    keyCode = keyCode,
                    metaState = metaState.value,
                    useShell = useShell.value,
                    device = chosenDevice.value
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
        keyCode: Result<Int>,
        useShell: Boolean,
        metaState: Int,
        chosenDevice: InputDeviceInfo?,
        inputDeviceList: List<InputDeviceInfo>
    ): ConfigKeyEventUiState {
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
            deviceListItems = inputDeviceList,
            chosenDeviceName = chosenDevice?.name ?: getString(R.string.from_no_device)
        )
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val getInputDevices: GetInputDevicesUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ConfigKeyEventViewModel(getInputDevices, resourceProvider) as T
        }
    }
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