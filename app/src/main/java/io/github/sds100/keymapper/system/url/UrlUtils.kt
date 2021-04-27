package io.github.sds100.keymapper.system.url

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.Result
import splitties.toast.toast

/**
 * Created by sds100 on 11/01/21.
 */

object UrlUtils {
    fun launchCustomTab(ctx: Context, url: String) {
        CustomTabsIntent.Builder()
            .build()
            .launchUrl(
                ctx,
                Uri.parse(url)
            )
    }

    fun openUrl(ctx: Context, url: String) {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            try {
                ctx.startActivity(this)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.error_no_app_found_to_open_url)
            }
        }
    }
}
