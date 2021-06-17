package io.github.sds100.keymapper.mappings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.UseCases
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 17/06/2021.
 */

class PauseMappingsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PAUSE_MAPPINGS = "io.github.sds100.keymapper.ACTION_PAUSE_MAPPINGS"
        const val ACTION_RESUME_MAPPINGS = "io.github.sds100.keymapper.ACTION_RESUME_MAPPINGS"
        const val ACTION_TOGGLE_MAPPINGS = "io.github.sds100.keymapper.ACTION_TOGGLE_MAPPINGS"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        val useCase = UseCases.pauseMappings(context)

        when (intent?.action) {
            ACTION_PAUSE_MAPPINGS -> useCase.pause()
            ACTION_RESUME_MAPPINGS -> useCase.resume()
            ACTION_TOGGLE_MAPPINGS -> {
                if (useCase.isPaused.firstBlocking()) {
                    useCase.resume()
                } else {
                    useCase.pause()
                }
            }
        }
    }
}