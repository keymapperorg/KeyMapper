package io.github.sds100.keymapper.system.url

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.success

object UrlUtils {
    fun openUrl(ctx: Context, url: String): KMResult<*> {
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                ctx.startActivity(this)
                return success()
            } catch (e: ActivityNotFoundException) {
                return KMError.NoAppToOpenUrl
            }
        }
    }
}
