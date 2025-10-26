package io.github.sds100.keymapper.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.keymaps.EnableKeyMapsUseCase
import javax.inject.Inject

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE OR RENAME BECAUSE IT BREAKS THE API
@AndroidEntryPoint
class EnableKeyMapsBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var useCase: EnableKeyMapsUseCase

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        context ?: return
        intent?.action ?: return

        if (intent.action != Api.ACTION_ENABLE_KEY_MAP &&
            intent.action != Api.ACTION_DISABLE_KEY_MAP &&
            intent.action != Api.ACTION_TOGGLE_KEY_MAP
        ) {
            return
        }

        val keyMapUid = intent.getStringExtra(Api.EXTRA_KEYMAP_ID) ?: return

        when (intent.action) {
            Api.ACTION_ENABLE_KEY_MAP -> useCase.enable(keyMapUid)
            Api.ACTION_DISABLE_KEY_MAP -> useCase.disable(keyMapUid)
            Api.ACTION_TOGGLE_KEY_MAP -> useCase.toggle(keyMapUid)
        }
    }
}
