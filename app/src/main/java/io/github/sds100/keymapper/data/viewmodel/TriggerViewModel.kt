package io.github.sds100.keymapper.data.viewmodel

import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 24/11/20.
 */

abstract class TriggerViewModel(
    private val mCoroutine: CoroutineScope,
    private val mDeviceInfoRepository: DeviceInfoRepository
)