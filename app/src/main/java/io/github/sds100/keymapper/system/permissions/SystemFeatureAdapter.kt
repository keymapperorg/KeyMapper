package io.github.sds100.keymapper.system.permissions


interface SystemFeatureAdapter {
    fun hasSystemFeature(feature: String): Boolean
    fun getSystemFeatures(): List<String>
}
