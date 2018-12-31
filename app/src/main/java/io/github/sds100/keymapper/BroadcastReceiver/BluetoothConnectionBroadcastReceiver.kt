package io.github.sds100.keymapper.BroadcastReceiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Services.MyIMEService
import io.github.sds100.keymapper.Utils.ImeUtils
import io.github.sds100.keymapper.Utils.str
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 28/12/2018.
 */

/**
 * Listens for bluetooth devices to connect/disconnect
 */
class BluetoothConnectionBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val KEY_DEFAULT_IME = "key_default_ime"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        context!!.apply {
            if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
                    intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {

                //get the properties of the device which just connected/disconnected
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return

                with(defaultSharedPreferences) {

                    //get the bluetooth devices chosen by the user. return if no bluetooth devices are chosen
                    val selectedDevices =
                            getStringSet(str(R.string.key_pref_bluetooth_devices), null) ?: return

                    //don't show the dialog if the user hasn't selected this device
                    if (selectedDevices.contains(device.address)) {
                        val automaticallySwitchIme =
                                getBoolean(str(R.string.key_pref_auto_change_ime_on_connect_disconnect), true)

                        if (automaticallySwitchIme) automaticallySwitchIme(context, intent.action!!)

                        val showIMEPickerAutomatically =
                                getBoolean(str(R.string.key_pref_auto_show_ime_picker), true)

                        //only show the dialog automatically if the user wants it to.
                        if (showIMEPickerAutomatically) ImeUtils.showInputMethodPickerDialogOutsideApp(context)
                    }
                }
            }
        }
    }

    private fun automaticallySwitchIme(ctx: Context, intentAction: String) {
        when (intentAction) {
            //when a device is connected, change to the Key Mapper ime
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val defaultIme = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

                ctx.defaultSharedPreferences.edit().putString(KEY_DEFAULT_IME, defaultIme).apply()

                ImeUtils.switchIme(MyIMEService.getImeId(ctx))
            }

            //when a device is disconnected, change back to the old ime
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                ImeUtils.switchIme(ctx.defaultSharedPreferences.getString(KEY_DEFAULT_IME, "")!!)
            }
        }
    }
}