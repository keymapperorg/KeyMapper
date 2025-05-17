package io.github.sds100.keymapper.system.intents

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentAdapterImpl @Inject constructor(@ApplicationContext private val context: Context) : IntentAdapter {
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
