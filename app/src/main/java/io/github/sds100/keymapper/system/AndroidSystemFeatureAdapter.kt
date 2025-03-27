package io.github.sds100.keymapper.system

import android.content.Context
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter

/**
 * Created by sds100 on 17/03/2021.
 */
class AndroidSystemFeatureAdapter(context: Context) : SystemFeatureAdapter {
    private val ctx = context.applicationContext

    override fun hasSystemFeature(feature: String): Boolean = ctx.packageManager.hasSystemFeature(feature)

    override fun getSystemFeatures(): List<String> {
        return ctx.packageManager.systemAvailableFeatures.map { it.name }
    }
}
