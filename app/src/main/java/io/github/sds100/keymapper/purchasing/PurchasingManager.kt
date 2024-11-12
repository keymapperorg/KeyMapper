package io.github.sds100.keymapper.purchasing

import io.github.sds100.keymapper.util.Result

interface PurchasingManager {
    suspend fun launchPurchasingFlow(product: ProductId): Result<Unit>
    suspend fun getProductPrice(product: ProductId): Result<String>
    suspend fun isPurchased(product: ProductId): Result<Boolean>
}
