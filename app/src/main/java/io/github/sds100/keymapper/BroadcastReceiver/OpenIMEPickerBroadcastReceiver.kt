package io.github.sds100.keymapper.BroadcastReceiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.inputmethod.InputMethodManager
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.RootUtils
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 30/09/2018.
 */

/**
 * Opens the IME picker when a bluetooth device is connected and disconnected
 */
class OpenIMEPickerBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SHOW_IME_PICKER = "action_show_ime_picker"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null &&
                (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
                        intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
                        intent.action == ACTION_SHOW_IME_PICKER)) {

            //get the properties of the device which just connected/disconnected
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            if (device != null) {
                with(context!!.defaultSharedPreferences) {

                    val showIMEPickerAutomatically =
                            getBoolean(context.getString(R.string.key_pref_auto_show_ime_picker), true)

                    //only show the dialog automatically if the user wants it to.
                    if (!showIMEPickerAutomatically) return

                    //get the bluetooth devices chosen by the user
                    val selectedDevices =
                            getStringSet(context.getString(R.string.key_pref_bluetooth_devices), null)

                    //if the user hasn't chosen any devices
                    if (selectedDevices == null) return

                    //don't show the dialog if the user hasn't selected this device
                    if (!selectedDevices.contains(device.address)) return
                }
            }

            /* Android 8.1 and higher don't seem to allow you to open the input method picker dialog
                 * from outside the app :( but it can be achieved by sending a broadcast with a
                 * system process id (requires root access) */
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                showInputMethodPickerDialog(context!!)
            } else {
                val command =
                        "am broadcast -a com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER"
                RootUtils.executeRootCommand(command)
            }
        }
    }

    private fun showInputMethodPickerDialog(ctx: Context) {
        val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        imeManager.showInputMethodPicker()
    }
}