package io.github.sds100.keymapper

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 31/03/2019.
 */

class DeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)

        context?.toast("Enabled")
    }

    override fun onDisabled(context: Context?, intent: Intent?) {
        super.onDisabled(context, intent)

        context?.toast("disabled")
    }
}