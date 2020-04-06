package io.github.sds100.keymapper.broadcastreceiver

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.KeyMapperImeService
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.result.onSuccess

/**
 * Created by sds100 on 28/12/2018.
 */

/**
 * Listens for bluetooth devices to connect/disconnect
 */
class BluetoothConnectionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
            intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {

            //get the properties of the device which just connected/disconnected
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return

            //get the bluetooth devices chosen by the user. return if no bluetooth devices are chosen
            val selectedDevices = AppPreferences.bluetoothDevices ?: return

            //don't show the dialog if the user hasn't selected this device
            if (selectedDevices.contains(device.address)) {
                val automaticallySwitchIme = AppPreferences.autoChangeImeOnBtConnect
                val haveWriteSecureSettingsPermission =
                    isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)

                if (automaticallySwitchIme && haveWriteSecureSettingsPermission) {
                    automaticallySwitchIme(context!!, intent.action!!)
                }

                val showIMEPickerAutomatically = AppPreferences.autoShowImePicker

                //only show the dialog automatically if the user wants it to.
                if (showIMEPickerAutomatically && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    KeyboardUtils.showInputMethodPickerDialogOutsideApp()
                }
            }
        }
    }

    private fun automaticallySwitchIme(ctx: Context, intentAction: String) {
        when (intentAction) {
            //when a device is connected, change to the Key Mapper ime
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val defaultIme = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

                AppPreferences.defaultIme = defaultIme

                KeyMapperImeService.getImeId().onSuccess {
                    KeyboardUtils.switchIme(it)
                }
            }

            //when a device is disconnected, change back to the old ime
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                AppPreferences.defaultIme?.let {
                    KeyboardUtils.switchIme(it)
                }
            }
        }
    }
}