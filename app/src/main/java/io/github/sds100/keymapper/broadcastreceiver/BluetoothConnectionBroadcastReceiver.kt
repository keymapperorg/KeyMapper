package io.github.sds100.keymapper.broadcastreceiver

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 28/12/2018.
 */

/**
 * Listens for bluetooth devices to connect/disconnect
 */
class BluetoothConnectionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
            intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {

            //get the properties of the device which just connected/disconnected
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                ?: return

            //get the bluetooth devices chosen by the user. return if no bluetooth devices are chosen
            val selectedDevices = context.globalPreferences
                .getFlow(Keys.bluetoothDevicesThatToggleKeymaps).firstBlocking() ?: return

            //don't show the dialog if the user hasn't selected this device
            if (selectedDevices.contains(device.address)) {
                val automaticallySwitchIme = context.globalPreferences
                    .getFlow(Keys.autoChangeImeOnBtConnect).firstBlocking() ?: false

                val haveWriteSecureSettingsPermission =
                    isPermissionGranted(context, Manifest.permission.WRITE_SECURE_SETTINGS)

                if (automaticallySwitchIme && haveWriteSecureSettingsPermission) {
                    automaticallySwitchIme(context, intent.action!!)
                }

                val showIMEPickerAutomatically = context.globalPreferences
                    .getFlow(Keys.autoShowImePicker).firstBlocking() ?: false

                //only show the dialog automatically if the user wants it to.
                if (showIMEPickerAutomatically && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    KeyboardUtils.showInputMethodPickerDialogOutsideApp(context)
                }
            }
        }
    }

    private fun automaticallySwitchIme(ctx: Context, intentAction: String) {
        when (intentAction) {
            //when a device is connected, change to the Key Mapper ime
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                KeyboardUtils.chooseCompatibleInputMethod(ctx)
            }

            //when a device is disconnected, change back to the old ime
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                KeyboardUtils.chooseLastUsedIncompatibleInputMethod(ctx)
            }
        }
    }
}