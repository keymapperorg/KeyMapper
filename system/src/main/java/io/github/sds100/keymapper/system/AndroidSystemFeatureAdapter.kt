package io.github.sds100.keymapper.system

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter

@Singleton
class AndroidSystemFeatureAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemFeatureAdapter {
    private val ctx = context.applicationContext

    override fun hasSystemFeature(feature: String): Boolean = ctx.packageManager.hasSystemFeature(feature)

    override fun getSystemFeatures(): List<String> {
        return ctx.packageManager.systemAvailableFeatures.map { it.name }
    }
}
