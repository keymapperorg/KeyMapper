package io.github.sds100.keymapper.base.input

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.Q)
class EvdevHandleCache(
    private val coroutineScope: CoroutineScope,
    private val devicesAdapter: DevicesAdapter,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager
) {
    private val devicesByPath: StateFlow<Map<String, EvdevDeviceHandle>> =
        combine(
            devicesAdapter.connectedInputDevices,
            systemBridgeConnectionManager.isConnected
        ) { _, isConnected ->
            if (!isConnected) {
                return@combine emptyMap()
            }

            // Do it on a separate thread in case there is deadlock
            withContext(Dispatchers.IO) {
                systemBridgeConnectionManager.run { bridge -> bridge.evdevInputDevices.associateBy { it.path } }
            }.onFailure { error ->
                Timber.e("Failed to get evdev input devices from system bridge $error")
            }.valueIfFailure { emptyMap() }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    fun getDevices(): List<EvdevDeviceInfo> {
        return devicesByPath.value.values.map { device ->
            EvdevDeviceInfo(
                name = device.name,
                bus = device.bus,
                vendor = device.vendor,
                product = device.product,
            )
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
}
