package io.github.sds100.keymapper.system.notifications

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.Constants

/**
 * Created by sds100 on 30/09/2018.
 */

object NotificationUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    fun openChannelSettings(ctx: Context, channelId: String) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, Constants.PACKAGE_NAME)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            ctx.startActivity(this)
        }
    }
}
