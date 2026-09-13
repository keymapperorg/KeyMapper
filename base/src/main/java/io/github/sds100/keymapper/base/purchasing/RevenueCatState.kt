package io.github.sds100.keymapper.base.purchasing

data class RevenueCatState(
    val entitlements: Set<RevenueCatEntitlementId>,
    val offeringMetadata: Map<String, Any>,
)
