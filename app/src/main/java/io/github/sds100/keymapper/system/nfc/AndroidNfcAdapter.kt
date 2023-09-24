package io.github.sds100.keymapper.system.nfc

import android.content.Context
import android.nfc.NfcManager
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 24/04/2021.
 */
class AndroidNfcAdapter(
    context: Context,
    private val suAdapter: SuAdapter
) : NfcAdapter {
    private val ctx = context.applicationContext

    private val nfcManager: NfcManager by lazy { ctx.getSystemService()!! }

    override fun isEnabled(): Boolean {
        return nfcManager.defaultAdapter.isEnabled
    }

    override fun enable(): Result<*> {
        return suAdapter.execute("svc nfc enable")
    }

    override fun disable(): Result<*> {
        return suAdapter.execute("svc nfc disable")
    }
}