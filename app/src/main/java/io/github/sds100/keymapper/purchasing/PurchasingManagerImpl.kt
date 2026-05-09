package io.github.sds100.keymapper.purchasing

import io.github.sds100.keymapper.base.purchasing.PurchasingError
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.base.purchasing.RevenueCatEntitlementId
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class PurchasingManagerImpl : PurchasingManager {
    override val onCompleteProductPurchase: MutableSharedFlow<RevenueCatEntitlementId> =
        MutableSharedFlow()
    override val entitlements: Flow<State<KMResult<Set<RevenueCatEntitlementId>>>> =
        MutableStateFlow(State.Data(PurchasingError.PurchasingNotImplemented))

    override suspend fun launchPurchasingFlow(
        packageId: String,
        verifyEntitlements: Array<RevenueCatEntitlementId>,
    ): KMResult<Unit> {
        return PurchasingError.PurchasingNotImplemented
    }

    override suspend fun isPackagePurchased(packageId: String): KMResult<Boolean> {
        return PurchasingError.PurchasingNotImplemented
    }

    override suspend fun getNonSubscriptionPurchaseCount(packageId: String): KMResult<Int> {
        return PurchasingError.PurchasingNotImplemented
    }

    override suspend fun getPackagePrice(packageId: String): KMResult<String> {
        return PurchasingError.PurchasingNotImplemented
    }

    override suspend fun hasEntitlement(entitlement: RevenueCatEntitlementId): KMResult<Boolean> {
        return PurchasingError.PurchasingNotImplemented
    }

    override suspend fun getCurrentOfferingId(): KMResult<String?> {
        return PurchasingError.PurchasingNotImplemented
    }

    override fun refresh() {}

    override fun trackCustomPaywallImpression(paywallIdentifier: String) {
        // Purchasing is not available in FOSS.
    }
}
