package io.github.sds100.keymapper.system.intents

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import javax.inject.Inject

/**
 * Created by sds100 on 21/04/2021.
 */

class IntentAdapterImpl @Inject constructor(@ApplicationContext context: Context) : IntentAdapter {
    private val ctx = context.applicationContext

    override fun send(target: IntentTarget, uri: String): Result<*> {
        val intent = Intent.parseUri(uri, 0)

        try {
            when (target) {
                IntentTarget.ACTIVITY -> {
                    if (intent.flags == 0) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    ctx.startActivity(intent)
                }
                IntentTarget.BROADCAST_RECEIVER -> ctx.sendBroadcast(intent)
                IntentTarget.SERVICE -> ctx.startService(intent)
            }
            return Success(Unit)
        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }
}

interface IntentAdapter {
    fun send(target: IntentTarget, uri: String): Result<*>
}