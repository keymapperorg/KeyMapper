package io.github.sds100.keymapper.base.purchasing

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface PurchasingManager {
    val onCompleteProductPurchase: MutableSharedFlow<RevenueCatEntitlementId>
    val entitlements: Flow<State<KMResult<Set<RevenueCatEntitlementId>>>>

    suspend fun launchPurchasingFlow(
        packageId: String,
        verifyEntitlements: Array<RevenueCatEntitlementId>,
    ): KMResult<Unit>

    suspend fun getPackagePrice(packageId: String): KMResult<String>
    suspend fun getCurrentOfferingId(): KMResult<String?>

    /**
     * Ask the store to restore the purchases made with the store account that is currently signed
     * in. This must only be called from an explicit user action, such as tapping a "restore
     * purchases" button, because it can show OS level sign-in prompts.
     *
     * Returns the entitlements the customer has after restoring, which is empty if the store
     * account has no purchases.
     */
    suspend fun restorePurchases(): KMResult<Set<RevenueCatEntitlementId>>

    /**
     * The id identifying this customer in RevenueCat so it can be looked up when they ask for
     * help. This fails if purchasing isn't implemented.
     */
    suspend fun getCustomerId(): KMResult<String>
    suspend fun isPackagePurchased(packageId: String): KMResult<Boolean>
    suspend fun getNonSubscriptionPurchaseCount(packageId: String): KMResult<Int>
    suspend fun hasEntitlement(entitlement: RevenueCatEntitlementId): KMResult<Boolean>
    fun refresh()

    /**
     * Reports a custom paywall view to RevenueCat (Play build only). No-op in FOSS.
     * [paywallIdentifier] is the paywall id in RevenueCat custom impression analytics.
     */
    fun trackCustomPaywallImpression(paywallIdentifier: String)
}
