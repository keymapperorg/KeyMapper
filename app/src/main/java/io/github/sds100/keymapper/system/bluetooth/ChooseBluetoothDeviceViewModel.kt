package io.github.sds100.keymapper.system.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by sds100 on 07/04/2021.
 */
@HiltViewModel
class ChooseBluetoothDeviceViewModel @Inject constructor(
    val useCase: ChooseBluetoothDeviceUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    private val _caption = MutableStateFlow<String?>(null)
    val caption: StateFlow<String?> = _caption

    private val _listItems: MutableStateFlow<State<List<ListItem>>> = MutableStateFlow(State.Loading)
    val listItems: StateFlow<State<List<ListItem>>> = _listItems.asStateFlow()

    private val _returnResult = MutableSharedFlow<BluetoothDeviceInfo>()
    val returnResult = _returnResult.asSharedFlow()

    private val missingPermissionListItem: TextListItem.Error by lazy {
        TextListItem.Error("missing_permission", getString(R.string.error_choose_bluetooth_devices_permission_denied))
    }

    init {
        combine(useCase.devices, useCase.hasPermissionToSeeDevices) { devices, permissionGranted ->
            if (!permissionGranted) {
                _caption.value = null
                _listItems.value = State.Data(listOf(missingPermissionListItem))
            } else {
                val devicesListItems = devices.map { device ->
                    DefaultSimpleListItem(
                        id = device.address,
                        title = device.name
                    )
                }

                _caption.value = if (devices.isEmpty()) {
                    getString(R.string.caption_no_paired_bt_devices)
                } else {
                    null
                }

                _listItems.value = State.Data(devicesListItems)
            }
        }.launchIn(viewModelScope)
    }

    fun onFixMissingPermissionListItemClick() {
        useCase.requestPermission()
    }

    fun onBluetoothDeviceListItemClick(id: String) {
        viewModelScope.launch {
            val deviceInfo = useCase.devices.value.find { it.address == id } ?: return@launch
            _returnResult.emit(deviceInfo)
        }
    }
}