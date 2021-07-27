package io.github.sds100.keymapper.system.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SimpleListItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 07/04/2021.
 */
class ChooseBluetoothDeviceViewModel(
    val useCase: ChooseBluetoothDeviceUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider {

    private val _caption = MutableStateFlow<String?>(null)
    val caption: StateFlow<String?> = _caption

    val listItems: StateFlow<State<List<SimpleListItem>>> = useCase.devices
        .map { deviceInfoList ->
            deviceInfoList.map { device ->
                DefaultSimpleListItem(
                    id = device.address,
                    title = device.name
                )
            }
        }
        .map { State.Data(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, State.Loading)

    private val _returnResult = MutableSharedFlow<BluetoothDeviceInfo>()
    val returnResult = _returnResult.asSharedFlow()

    init {
        viewModelScope.launch {
            useCase.devices.collectLatest { devices ->
                _caption.value = if (devices.isEmpty()) {
                    getString(R.string.caption_no_paired_bt_devices)
                } else {
                    null
                }
            }
        }
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            val deviceInfo = useCase.devices.value.find { it.address == id } ?: return@launch
            _returnResult.emit(deviceInfo)
        }
    }

    class Factory(
        private val useCase: ChooseBluetoothDeviceUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ChooseBluetoothDeviceViewModel(
                useCase,
                resourceProvider
            ) as T
    }
}