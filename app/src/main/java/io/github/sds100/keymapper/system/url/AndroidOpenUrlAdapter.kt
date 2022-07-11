package io.github.sds100.keymapper.system.url

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by sds100 on 24/04/2021.
 */
@Singleton
class AndroidOpenUrlAdapter @Inject constructor(@ApplicationContext context: Context) : OpenUrlAdapter {

    private val ctx = context.applicationContext

    override fun openUrl(url: String): Result<*> {
        return UrlUtils.openUrl(ctx, url)
    }
}