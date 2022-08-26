package io.github.sds100.keymapper.actions.keyevent

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import kotlinx.coroutines.flow.*
import splitties.bitflags.withFlag
import javax.inject.Inject

/**
 * Created by sds100 on 30/03/2020.
 */

@HiltViewModel
class ConfigKeyEventViewModel @Inject constructor(
    private val useCase: ConfigKeyEventUseCase
) : ViewModel() {

    private val keyCodeText: MutableStateFlow<String> = MutableStateFlow("")
    private val selectedDevice: MutableStateFlow<String?> = MutableStateFlow(null)
    private val selectedModifiers: MutableStateFlow<Set<Int>> = MutableStateFlow(emptySet())

    val state: StateFlow<ConfigKeyEventState> =
        combine(
            keyCodeText,
            selectedDevice,
            selectedModifiers,
            useCase.inputDevices,
            useCase.showDeviceDescriptors
        ) { keyCode, selectedDevice, selectedModifiers, devices, showDeviceDescriptors ->
            buildUiState(keyCode, selectedDevice, selectedModifiers, devices, showDeviceDescriptors)
        }.stateIn(viewModelScope, SharingStarted.Lazily, ConfigKeyEventState.EMPTY)

    fun onKeyCodeTextChange(text: String) {
        keyCodeText.update { text }
    }

    fun onSelectNoDevice() {
        selectedDevice.value = null
    }

    fun onSelectDevice(id: String) {
        selectedDevice.value = id
    }

    fun onSelectModifier(modifier: Int) {
        selectedModifiers.update { it.plus(modifier) }
    }

    fun onDeselectModifier(modifier: Int) {
        selectedModifiers.update { it.minus(modifier) }
    }

    fun createResult(): ActionData.InputKeyEvent? {
        state.value.also { state ->
            val keyCode = state.keyCode.toIntOrNull() ?: return null
            var metaState = 0

            state.selectedModifiers.forEach { modifier ->
                metaState = metaState.withFlag(modifier)
            }

            val device = state.selectedDevice?.let { selectedDevice ->
                ActionData.InputKeyEvent.Device(selectedDevice.id, selectedDevice.name)
            }

            return ActionData.InputKeyEvent(keyCode, metaState, device = device)
        }
    }

    fun onChooseKeyCode(keyCode: Int) {
        keyCodeText.update { keyCode.toString() }
    }

    private fun buildUiState(
        keyCode: String,
        selectedDevice: String?,
        selectedModifiers: Set<Int>,
        devices: List<InputDeviceInfo>,
        showDeviceDescriptors: Boolean
    ): ConfigKeyEventState {
        val isValidKeyCode = keyCode.toIntOrNull() != null
        val keyCodeName = if (isValidKeyCode) {
            KeyEvent.keyCodeToString(keyCode.toInt())
        } else {
            ""
        }

        val keyCodeError = when {
            keyCode.isEmpty() -> KeyCodeError.EMPTY
            !isValidKeyCode -> KeyCodeError.NOT_NUMBER
            else -> KeyCodeError.NONE
        }

        val deviceItems = devices.map { device ->
            val name = if (showDeviceDescriptors) {
                InputDeviceUtils.appendDeviceDescriptorToName(device.descriptor, device.name)
            } else {
                device.name
            }

            DeviceItem(device.descriptor, name)
        }
        val selectedDeviceItem = deviceItems.find { it.id == selectedDevice }

        return ConfigKeyEventState(
            keyCode = keyCode,
            keyCodeName = keyCodeName,
            keyCodeError = keyCodeError,
            selectedModifiers = selectedModifiers,
            selectedDevice = selectedDeviceItem,
            devices = deviceItems,
            isDoneButtonEnabled = keyCodeError == KeyCodeError.NONE
        )
    }
}

data class DeviceItem(val id: String, val name: String)
enum class KeyCodeError {
    NONE,
    EMPTY,
    NOT_NUMBER
}

data class ConfigKeyEventState(
    val keyCode: String,
    val keyCodeName: String,
    val keyCodeError: KeyCodeError,
    val selectedModifiers: Set<Int>,
    val devices: List<DeviceItem>,
    val selectedDevice: DeviceItem?,
    val isDoneButtonEnabled: Boolean
) {
    companion object {
        val EMPTY: ConfigKeyEventState = ConfigKeyEventState(
            keyCode = "",
            keyCodeName = "",
            keyCodeError = KeyCodeError.EMPTY,
            selectedModifiers = emptySet(),
            devices = emptyList(),
            selectedDevice = null,
            isDoneButtonEnabled = false
        )
    }
}