package io.github.sds100.keymapper.system.nfc

import android.content.Context
import android.nfc.NfcManager
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class AndroidNfcAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val suAdapter: SuAdapter,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
) : NfcAdapter {
    private val ctx = context.applicationContext

    private val nfcManager: NfcManager? by lazy { ctx.getSystemService() }

    override fun isEnabled(): Boolean = nfcManager?.defaultAdapter?.isEnabled ?: false

    override fun enable(): KMResult<*> {
        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            return systemBridgeConnectionManager.run { bridge -> bridge.setNfcEnabled(true) }
        } else {
            return runBlocking { suAdapter.execute("svc nfc enable") }
        }
    }

    override fun disable(): KMResult<*> {
        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            return systemBridgeConnectionManager.run { bridge -> bridge.setNfcEnabled(false) }
        } else {
            return runBlocking { suAdapter.execute("svc nfc disable") }
        }
    }
}
