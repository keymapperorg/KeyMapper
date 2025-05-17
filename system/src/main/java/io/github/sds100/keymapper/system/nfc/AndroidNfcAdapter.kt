package io.github.sds100.keymapper.system.nfc

import android.content.Context
import android.nfc.NfcManager
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.system.root.SuAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNfcAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val suAdapter: SuAdapter,
) : NfcAdapter {
    private val ctx = context.applicationContext

    private val nfcManager: NfcManager by lazy { ctx.getSystemService()!! }

    override fun isEnabled(): Boolean = nfcManager.defaultAdapter.isEnabled

    override fun enable(): Result<*> = suAdapter.execute("svc nfc enable")

    override fun disable(): Result<*> = suAdapter.execute("svc nfc disable")
}
