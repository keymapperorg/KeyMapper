package io.github.sds100.keymapper.base.system.permissions

import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter

class FakeSystemFeatureAdapter : SystemFeatureAdapter {
    var features: List<String> = emptyList()

    override fun hasSystemFeature(feature: String): Boolean {
        return feature in features
    }

    override fun getSystemFeatures(): List<String> {
        return features
    }
}
