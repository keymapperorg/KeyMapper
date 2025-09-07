package io.github.sds100.keymapper.system.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBluetoothAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager
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
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

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

                    Timber.i("On Bluetooth device connected: $name")

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

                    Timber.i("On Bluetooth device disconnected: $name")

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
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

                    Timber.i("On Bluetooth device bond state changed to $bondState: $name")

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

    override fun enable(): KMResult<*> {
        if (adapter == null) {
            return KMError.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            return systemBridgeConnectionManager.run { bridge -> bridge.setBluetoothEnabled(true) }
        } else {
            adapter.enable()
            return Success(Unit)
        }

    }

    override fun disable(): KMResult<*> {
        if (adapter == null) {
            return KMError.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            return systemBridgeConnectionManager.run { bridge -> bridge.setBluetoothEnabled(false) }
        } else {
            adapter.disable()
            return Success(Unit)
        }
    }
}
