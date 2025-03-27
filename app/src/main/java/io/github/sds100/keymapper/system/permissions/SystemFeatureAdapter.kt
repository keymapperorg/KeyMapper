package io.github.sds100.keymapper.system.permissions

/**
 * Created by sds100 on 16/03/2021.
 */
interface SystemFeatureAdapter {
    fun hasSystemFeature(feature: String): Boolean
    fun getSystemFeatures(): List<String>
}
