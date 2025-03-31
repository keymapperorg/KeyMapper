package io.github.sds100.keymapper.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.UseCases
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 17/06/2021.
 */

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
class PauseMappingsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        val useCase = UseCases.pauseKeyMaps(context)

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
