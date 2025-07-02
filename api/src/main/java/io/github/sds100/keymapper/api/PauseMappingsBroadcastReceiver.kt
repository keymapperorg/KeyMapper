package io.github.sds100.keymapper.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.common.utils.firstBlocking
import javax.inject.Inject

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
@AndroidEntryPoint
class PauseMappingsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var useCase: PauseKeyMapsUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        when (intent?.action) {
            Api.ACTION_PAUSE_MAPPINGS -> useCase.pause()
            Api.ACTION_RESUME_MAPPINGS -> useCase.resume()
            Api.ACTION_TOGGLE_MAPPINGS -> {
                if (useCase.isPaused.firstBlocking()) {
                    useCase.resume()
                } else {
                    useCase.pause()
                }
            }
        }
    }
}
