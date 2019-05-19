package io.github.sds100.keymapper.util

import android.content.Context
import android.nfc.NfcManager

/**
 * Created by sds100 on 18/05/2019.
 */

object NfcUtils {
    fun enable() {
        RootUtils.executeRootCommand("svc nfc enable")
    }

    fun disable() {
        RootUtils.executeRootCommand("svc nfc disable")
    }

    fun toggle(ctx: Context) {
        if (isEnabled(ctx)) {
            disable()
        } else {
            enable()
        }
    }

    fun isEnabled(ctx: Context): Boolean {
        val nfcManager = ctx.getSystemService(Context.NFC_SERVICE) as NfcManager
        return nfcManager.defaultAdapter.isEnabled
    }
}