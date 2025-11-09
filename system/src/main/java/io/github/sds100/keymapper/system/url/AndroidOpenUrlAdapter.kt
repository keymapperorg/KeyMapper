package io.github.sds100.keymapper.system.url

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidOpenUrlAdapter @Inject constructor(@ApplicationContext private val context: Context) :
    OpenUrlAdapter {

    private val ctx = context.applicationContext

    override fun openUrl(url: String): KMResult<*> = UrlUtils.openUrl(ctx, url)
}
