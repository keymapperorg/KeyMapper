package io.github.sds100.keymapper.system

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by sds100 on 17/03/2021.
 */
@Singleton
class AndroidSystemFeatureAdapter @Inject constructor(@ApplicationContext context: Context) : SystemFeatureAdapter {
    private val ctx = context.applicationContext

    override fun hasSystemFeature(feature: String): Boolean {
        return ctx.packageManager.hasSystemFeature(feature)
    }
}