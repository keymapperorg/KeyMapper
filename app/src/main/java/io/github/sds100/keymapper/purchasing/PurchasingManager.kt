package io.github.sds100.keymapper.purchasing

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.MutableSharedFlow

interface PurchasingManager {
    val onCompleteProductPurchase: MutableSharedFlow<ProductId>
    suspend fun launchPurchasingFlow(product: ProductId): Result<Unit>
    suspend fun getProductPrice(product: ProductId): Result<String>
    suspend fun isPurchased(product: ProductId): Result<Boolean>
}
