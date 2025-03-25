package io.github.sds100.keymapper.system.url

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.success

/**
 * Created by sds100 on 11/01/21.
 */

object UrlUtils {
    fun openUrl(ctx: Context, url: String): Result<*> {
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                ctx.startActivity(this)
                return success()
            } catch (e: ActivityNotFoundException) {
                return Error.NoAppToOpenUrl
            }
        }
    }
}
