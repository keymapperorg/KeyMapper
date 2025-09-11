package io.github.sds100.keymapper.base.input

import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

@RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
class EvdevHandleCache(
    private val coroutineScope: CoroutineScope,
    private val devicesAdapter: DevicesAdapter,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
) {
    private val devicesByPath: MutableMap<String, EvdevDeviceHandle> = ConcurrentHashMap()

    init {
        coroutineScope.launch {
            combine(
                devicesAdapter.connectedInputDevices,
                systemBridgeConnectionManager.connectionState,
            ) { _, connectionState ->
                if (connectionState !is SystemBridgeConnectionState.Connected) {
                    devicesByPath.clear()
                } else {
                    invalidate()
                }
            }.collect()
        }
    }

    fun getDevices(): List<EvdevDeviceInfo> {
        return devicesByPath.values.map { device ->
            EvdevDeviceInfo(
                name = device.name,
                bus = device.bus,
                vendor = device.vendor,
                product = device.product,
            )
        }
    }

    fun getByPath(path: String): EvdevDeviceHandle? {
        return devicesByPath[path]
    }

    fun getByInfo(deviceInfo: EvdevDeviceInfo): EvdevDeviceHandle? {
        return devicesByPath.values.firstOrNull {
            it.name == deviceInfo.name &&
                it.bus == deviceInfo.bus &&
                it.vendor == deviceInfo.vendor &&
                it.product == deviceInfo.product
        }
    }

    suspend fun invalidate() {
        // Do it on a separate thread in case there is deadlock
        val newDevices = withContext(Dispatchers.IO) {
            systemBridgeConnectionManager.run { bridge -> bridge.evdevInputDevices.associateBy { it.path } }
        }.onFailure { error ->
            Timber.e("Failed to get evdev input devices from system bridge $error")
        }.valueIfFailure { emptyMap() }

        devicesByPath.clear()
        devicesByPath.putAll(newDevices)
    }
}
