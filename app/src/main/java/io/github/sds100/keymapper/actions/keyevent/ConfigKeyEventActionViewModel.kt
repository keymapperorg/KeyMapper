package io.github.sds100.keymapper.actions.keyevent

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import javax.inject.Inject

/**
 * Created by sds100 on 30/03/2020.
 */

@HiltViewModel
class ConfigKeyEventActionViewModel @Inject constructor(
    private val useCase: ConfigKeyEventUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    NavigationViewModel by NavigationViewModelImpl() {

    private val keyEventState = MutableStateFlow(KeyEventState())

    private val _uiState = MutableStateFlow(
        buildUiState(
            keyEventState.value,
            inputDeviceList = emptyList(),
            showDeviceDescriptors = false
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _returnResult = MutableSharedFlow<ActionData.InputKeyEvent>()
    val returnResult = _returnResult.asSharedFlow()

    private val rebuildUiState = MutableSharedFlow<Unit>()

    init {
        viewModelScope.launch {

            combine(
                keyEventState,
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
        val oldMetaState = keyEventState.value.metaState

        if (isChecked) {
            keyEventState.value = keyEventState.value.copy(
                metaState = oldMetaState.withFlag(modifier)
            )
        } else {
            keyEventState.value = keyEventState.value.copy(
                metaState = oldMetaState.minusFlag(modifier)
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
                it.descriptor == action.device?.descriptor
                    && it.name == action.device.name
            }

            keyEventState.value = KeyEventState(
                Success(action.keyCode),
                inputDevice,
                useShell = action.useShell,
                metaState = action.metaState
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
        viewModelScope.launch {
            val chosenDevice = uiState.value.deviceListItems.getOrNull(index)

            if (chosenDevice == null) {
                return@launch
            }

            keyEventState.value = keyEventState.value.copy(
                chosenDevice = chosenDevice
            )
        }
    }

    fun onDoneClick() {
        viewModelScope.launch {
            val keyCode = keyEventState.value.keyCode.valueOrNull() ?: return@launch

            val device = keyEventState.value.chosenDevice?.let {
                ActionData.InputKeyEvent.Device(
                    descriptor = it.descriptor,
                    name = it.name
                )
            }

            _returnResult.emit(
                ActionData.InputKeyEvent(
                    keyCode = keyCode,
                    metaState = keyEventState.value.metaState,
                    useShell = keyEventState.value.useShell,
                    device = device
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
            chosenDevice == null -> getString(R.string.create_key_event_screen_no_device)
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