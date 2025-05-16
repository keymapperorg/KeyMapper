package io.github.sds100.keymapper.system.url

import android.content.Context
import io.github.sds100.keymapper.common.result.Result

class AndroidOpenUrlAdapter(context: Context) : OpenUrlAdapter {

    private val ctx = context.applicationContext

    override fun openUrl(url: String): Result<*> = UrlUtils.openUrl(ctx, url)
}
