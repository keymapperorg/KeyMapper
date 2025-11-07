package android.nfc

import android.os.Build

object NfcAdapterApis {
    fun enable(adapter: INfcAdapter, packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            adapter.enable(packageName)
        } else {
            adapter.enable()
        }
    }

    fun disable(adapter: INfcAdapter, saveState: Boolean, packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            adapter.disable(saveState, packageName)
        } else {
            adapter.disable(saveState)
        }
    }
}
