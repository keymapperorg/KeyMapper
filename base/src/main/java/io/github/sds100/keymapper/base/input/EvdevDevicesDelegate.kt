package io.github.sds100.keymapper.base.input

import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.models.GrabbedDeviceHandle
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Need to use a cache that maps a device id to the other device information. This information
 * could be sent in the onEvdevEvent callback instead, but sending non-primitive strings for the
 * device name introduces extra overhead across Binder and JNI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
@Singleton
class EvdevDevicesDelegate @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val devicesAdapter: DevicesAdapter,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
) {
    private val grabbedDevicesById: MutableStateFlow<Map<Int, EvdevDeviceInfo>> =
        MutableStateFlow(emptyMap())

    // Use a channel so there are no race conditions when grabbing and that all
    // grab operations finish in the correct order to completion.
    private val grabDevicesChannel: Channel<List<EvdevDeviceInfo>> = Channel(capacity = 16)

    // All the evdev devices on the device, regardless of whether they are grabbed.
    val allDevices: MutableStateFlow<List<EvdevDeviceInfo>> = MutableStateFlow(emptyList())

    init {
        coroutineScope.launch {
            // Only listen to changes in the connected input devices if the system bridge
            // is connected.
            systemBridgeConnectionManager.connectionState.flatMapLatest { connectionState ->
                when (connectionState) {
                    is SystemBridgeConnectionState.Connected -> {
                        devicesAdapter.connectedInputDevices.onEach {
                            allDevices.value = fetchAllDevices()
                        }
                    }
                    is SystemBridgeConnectionState.Disconnected -> {
                        allDevices.value = emptyList()
                        grabbedDevicesById.value = emptyMap()
                        emptyFlow()
                    }
                }
            }.collect()
        }

        // Process on another thread because system bridge grabbing calls are blocking
        coroutineScope.launch(Dispatchers.IO) {
            grabDevicesChannel.receiveAsFlow().collect { devices ->
                systemBridgeConnectionManager
                    .run { bridge -> bridge.setGrabbedDevices(devices.toTypedArray()) }
                    .onSuccess { grabbedDevices ->
                        onGrabbedDevicesChanged(grabbedDevices?.filterNotNull() ?: emptyList())
                    }.onFailure { error ->
                        Timber.w(
                            "Grabbing devices failed in system bridge: $error",
                        )
                    }
            }
        }
    }

    fun setGrabbedDevices(devices: List<EvdevDeviceInfo>) {
        grabDevicesChannel.trySend(devices)
    }

    fun getGrabbedDeviceInfo(id: Int): EvdevDeviceInfo? {
        return grabbedDevicesById.value[id]
    }

    fun getGrabbedDevices(): List<EvdevDeviceInfo> {
        return grabbedDevicesById.value.values.toList()
    }

    private fun onGrabbedDevicesChanged(devices: List<GrabbedDeviceHandle>) {
        Timber.i("Grabbed devices changed: [${devices.joinToString { it.name }}]")

        grabbedDevicesById.value =
            devices.associate { handle ->
                handle.id to
                    EvdevDeviceInfo(handle.name, handle.bus, handle.vendor, handle.product)
            }
    }

    private suspend fun fetchAllDevices(): List<EvdevDeviceInfo> {
        // Do it on a separate thread in case there is deadlock
        return withContext(Dispatchers.IO) {
            systemBridgeConnectionManager.run { bridge ->
                bridge.evdevInputDevices?.filterNotNull() ?: emptyList()
            }
        }.onFailure { error ->
            Timber.e("Failed to get evdev input devices from system bridge: $error")
        }.valueIfFailure { emptyList() }
    }
}
