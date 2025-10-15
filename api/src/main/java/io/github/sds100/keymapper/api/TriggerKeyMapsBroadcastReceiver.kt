package io.github.sds100.keymapper.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.keymaps.TriggerKeyMapEvent
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
@AndroidEntryPoint
class TriggerKeyMapsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var serviceAdapter: AccessibilityServiceAdapter

    @Inject
    lateinit var coroutineScope: CoroutineScope

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        when (intent.action) {
            Api.ACTION_TRIGGER_KEYMAP_BY_UID -> {
                intent.getStringExtra(Api.EXTRA_KEYMAP_ID)?.let { uid ->
                    coroutineScope.launch {
                        serviceAdapter.send(TriggerKeyMapEvent(uid))
                    }
                }
            }
        }
    }
}
