package io.github.sds100.keymapper.system.url

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.success



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
