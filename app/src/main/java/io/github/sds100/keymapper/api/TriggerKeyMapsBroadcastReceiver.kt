package io.github.sds100.keymapper.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.util.Event
import kotlinx.coroutines.runBlocking
import timber.log.Timber

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
class TriggerKeyMapsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        val serviceAdapter = ServiceLocator.accessibilityServiceAdapter(context)

        when (intent.action) {
            Api.ACTION_TRIGGER_KEYMAP_BY_UID -> {
                intent.getStringExtra(Api.EXTRA_KEYMAP_UID)?.let { uid ->
                    runBlocking {
                        serviceAdapter.send(Event.TriggerKeyMap(uid))
                    }
                }
            }
        }
    }
}
