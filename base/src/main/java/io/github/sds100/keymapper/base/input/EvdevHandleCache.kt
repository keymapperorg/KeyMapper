package io.github.sds100.keymapper.base.input

import android.os.RemoteException
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import timber.log.Timber

class EvdevHandleCache(
    private val coroutineScope: CoroutineScope,
    private val devicesAdapter: DevicesAdapter,
    private val systemBridge: StateFlow<ISystemBridge?>,
) {
    private val devicesByPath: StateFlow<Map<String, EvdevDeviceHandle>> =
        combine(devicesAdapter.connectedInputDevices, systemBridge) { _, systemBridge ->
            systemBridge ?: return@combine emptyMap<String, EvdevDeviceHandle>()

            try {
                // Do it on a separate thread in case there is deadlock
                withContext(Dispatchers.IO) {
                    systemBridge.evdevInputDevices.associateBy { it.path }
                }
            } catch (e: RemoteException) {
                Timber.e("Failed to get evdev input devices from system bridge $e")
                emptyMap()
            }
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
