package io.github.sds100.keymapper.base.input

import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.manager.isConnected
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
@Singleton
class EvdevHandleCache @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val devicesAdapter: DevicesAdapter,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
) {
    private val devicesByPath: MutableStateFlow<Map<String, EvdevDeviceHandle>> =
        MutableStateFlow(emptyMap())

    val devices: StateFlow<List<EvdevDeviceInfo>> =
        devicesByPath
            .map { pathMap ->
                pathMap.values.map { device ->
                    EvdevDeviceInfo(
                        name = device.name,
                        bus = device.bus,
                        vendor = device.vendor,
                        product = device.product,
                    )
                }
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    init {
        coroutineScope.launch {
            combine(
                devicesAdapter.connectedInputDevices,
                systemBridgeConnectionManager.connectionState,
            ) { _, connectionState ->
                if (connectionState is SystemBridgeConnectionState.Connected) {
                    invalidate()
                } else {
                    devicesByPath.value = emptyMap()
                }
            }.collect()
        }
    }

    fun getByPath(path: String): EvdevDeviceHandle? {
        return devicesByPath.value[path]
    }

    fun getByInfo(deviceInfo: EvdevDeviceInfo): EvdevDeviceHandle? {
        return devicesByPath.value.values.firstOrNull {
            it.name == deviceInfo.name &&
                it.bus == deviceInfo.bus &&
                it.vendor == deviceInfo.vendor &&
                it.product == deviceInfo.product
        }
    }

    suspend fun invalidate() {
        if (!systemBridgeConnectionManager.isConnected()) {
            devicesByPath.value = emptyMap()
            return
        }

        // Do it on a separate thread in case there is deadlock
        val newDevices = withContext(Dispatchers.IO) {
            systemBridgeConnectionManager.run { bridge ->
                bridge.evdevInputDevices.associateBy { it.path }
            }
        }.onFailure { error ->
            Timber.e("Failed to get evdev input devices from system bridge $error")
        }.valueIfFailure { emptyMap() }

        devicesByPath.value = newDevices
    }
}
