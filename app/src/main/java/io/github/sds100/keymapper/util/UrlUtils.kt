package io.github.sds100.keymapper.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import io.github.sds100.keymapper.R

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
}