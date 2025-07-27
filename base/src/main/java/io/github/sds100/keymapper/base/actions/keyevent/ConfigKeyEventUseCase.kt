package io.github.sds100.keymapper.base.actions.keyevent

import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConfigKeyEventUseCaseImpl @Inject constructor(
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
