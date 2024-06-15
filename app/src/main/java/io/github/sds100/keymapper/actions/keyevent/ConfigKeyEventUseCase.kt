package io.github.sds100.keymapper.actions.keyevent

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 01/05/2021.
 */

class ConfigKeyEventUseCaseImpl(
    private val preferenceRepository: PreferenceRepository,
    private val devicesAdapter: DevicesAdapter,
) : ConfigKeyEventUseCase {
    override val inputDevices: Flow<List<InputDeviceInfo>> =
        devicesAdapter.connectedInputDevices.map { state ->
            if (state !is State.Data) {
                emptyList()
            } else {
                state.data
            }
        }

    override val showDeviceDescriptors: Flow<Boolean> =
        preferenceRepository.get(Keys.showDeviceDescriptors).map { it ?: false }
}

interface ConfigKeyEventUseCase {
    val inputDevices: Flow<List<InputDeviceInfo>>
    val showDeviceDescriptors: Flow<Boolean>
}
