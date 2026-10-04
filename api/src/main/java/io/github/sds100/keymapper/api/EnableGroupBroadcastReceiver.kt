package io.github.sds100.keymapper.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.keymaps.EnableGroupUseCase
import javax.inject.Inject

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE OR RENAME BECAUSE IT BREAKS THE API
@AndroidEntryPoint
class EnableGroupBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var useCase: EnableGroupUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent?.action ?: return

        if (intent.action != Api.ACTION_ENABLE_GROUP &&
            intent.action != Api.ACTION_DISABLE_GROUP &&
            intent.action != Api.ACTION_TOGGLE_GROUP
        ) {
            return
        }

        val groupUid = intent.getStringExtra(Api.EXTRA_GROUP_UID) ?: return

        when (intent.action) {
            Api.ACTION_ENABLE_GROUP -> useCase.enable(groupUid)
            Api.ACTION_DISABLE_GROUP -> useCase.disable(groupUid)
            Api.ACTION_TOGGLE_GROUP -> useCase.toggle(groupUid)
        }
    }
}
