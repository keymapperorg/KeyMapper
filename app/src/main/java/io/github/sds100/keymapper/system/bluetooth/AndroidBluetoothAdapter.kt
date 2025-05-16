package io.github.sds100.keymapper.system.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AndroidBluetoothAdapter(
    context: Context,
    private val coroutineScope: CoroutineScope,
) : io.github.sds100.keymapper.system.bluetooth.BluetoothAdapter {

    private val bluetoothManager: BluetoothManager? = context.getSystemService()
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    override val onDeviceConnect = MutableSharedFlow<BluetoothDeviceInfo>()
    override val onDeviceDisconnect = MutableSharedFlow<BluetoothDeviceInfo>()
    override val onDevicePairedChange = MutableSharedFlow<BluetoothDeviceInfo>()
    override val isBluetoothEnabled =
        MutableStateFlow(adapter?.isEnabled ?: false)

    private val ctx: Context = context.applicationContext
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            onReceiveIntent(intent)
        }
    }

    init {
        IntentFilter().apply {
            // these broadcasts can't be received from a manifest declared receiver on Android 8.0+
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)

            ContextCompat.registerReceiver(
                ctx,
                broadcastReceiver,
                this,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    fun onReceiveIntent(intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return

                coroutineScope.launch {
                    val address = device.address ?: return@launch
                    val name = device.name ?: return@launch

                    onDeviceConnect.emit(
                        BluetoothDeviceInfo(
                            address = address,
                            name = name,
                        ),
                    )
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return

                coroutineScope.launch {
                    val address = device.address ?: return@launch
                    val name = device.name ?: return@launch

                    onDeviceDisconnect.emit(
                        BluetoothDeviceInfo(
                            address = address,
                            name = name,
                        ),
                    )
                }
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return

                coroutineScope.launch {
                    val address = device.address ?: return@launch
                    val name = device.name ?: return@launch

                    onDevicePairedChange.emit(
                        BluetoothDeviceInfo(
                            address = address,
                            name = name,
                        ),
                    )
                }
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)

                if (state == -1) return

                when (state) {
                    BluetoothAdapter.STATE_ON -> isBluetoothEnabled.value = true
                    BluetoothAdapter.STATE_OFF -> isBluetoothEnabled.value = false
                }
            }
        }
    }

    override fun enable(): Result<*> {
        if (adapter == null) {
            return Error.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
        }

        adapter.enable()

        return Success(Unit)
    }

    override fun disable(): Result<*> {
        if (adapter == null) {
            return Error.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
        }

        adapter.disable()

        return Success(Unit)
    }
}
