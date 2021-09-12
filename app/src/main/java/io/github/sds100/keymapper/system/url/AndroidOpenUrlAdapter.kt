package io.github.sds100.keymapper.system.url

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.success

/**
 * Created by sds100 on 24/04/2021.
 */
class AndroidOpenUrlAdapter(context: Context) : OpenUrlAdapter {

    private val ctx = context.applicationContext

    override fun openUrl(url: String): Result<*> {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
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