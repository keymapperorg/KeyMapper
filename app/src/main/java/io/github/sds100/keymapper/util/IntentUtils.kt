package io.github.sds100.keymapper.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.sds100.keymapper.broadcastreceiver.KeyMapperBroadcastReceiver

/**
 * Created by sds100 on 24/03/2019.
 */

object IntentUtils {

    fun createPendingBroadcastIntent(
        ctx: Context,
        action: String,
        cls: Class<*> = KeyMapperBroadcastReceiver::class.java,
        extras: Bundle = Bundle.EMPTY
    ): PendingIntent {
        val intent = Intent(ctx, cls).apply {
            setAction(action)
            putExtras(extras)
        }

        return PendingIntent.getBroadcast(ctx, 0, intent, 0)
    }

    fun createPendingActivityIntent(ctx: Context, activity: Class<*>): PendingIntent {
        val intent = Intent(ctx, activity)

        return PendingIntent.getActivity(ctx, 0, intent, 0)
    }
}