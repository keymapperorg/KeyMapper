package io.github.sds100.keymapper

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by sds100 on 31/03/2019.
 */

class DeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context?, intent: Intent?) {
        super.onDisabled(context, intent)
    }
}