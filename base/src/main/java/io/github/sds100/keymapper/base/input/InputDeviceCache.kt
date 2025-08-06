package io.github.sds100.keymapper.base.input

import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class InputDeviceCache(
    private val coroutineScope: CoroutineScope,
    private val devicesAdapter: DevicesAdapter
) {

    private val inputDevicesById: StateFlow<Map<Int, InputDeviceInfo>> =
        devicesAdapter.connectedInputDevices
            .filterIsInstance<State.Data<List<InputDeviceInfo>>>()
            .map { state -> state.data.associateBy { it.id } }
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    private val inputDevicesByDescriptor: StateFlow<Map<String, InputDeviceInfo>> =
        devicesAdapter.connectedInputDevices
            .filterIsInstance<State.Data<List<InputDeviceInfo>>>()
            .map { state -> state.data.associateBy { it.descriptor } }
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    fun getById(id: Int): InputDeviceInfo? {
        return inputDevicesById.value[id]
    }

    fun getByDescriptor(descriptor: String): InputDeviceInfo? {
        return inputDevicesByDescriptor.value.values.firstOrNull { it.descriptor == descriptor }
    }
}