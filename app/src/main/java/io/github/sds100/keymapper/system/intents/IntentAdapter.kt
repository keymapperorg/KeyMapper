package io.github.sds100.keymapper.system.intents

import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.Success

class IntentAdapterImpl(context: Context) : IntentAdapter {
    private val ctx = context.applicationContext

    override fun send(
        target: IntentTarget,
        uri: String,
        extras: List<IntentExtraModel>,
    ): Result<*> {
        val intent = Intent.parseUri(uri, 0)

        extras.forEach { e ->
            e.type.putInIntent(intent, e.name, e.value)
        }

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
    fun send(target: IntentTarget, uri: String, extras: List<IntentExtraModel>): Result<*>
}
