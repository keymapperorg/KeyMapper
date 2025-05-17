package io.github.sds100.keymapper.base.purchasing

import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface PurchasingManager {
    val onCompleteProductPurchase: MutableSharedFlow<ProductId>
    val purchases: Flow<State<Result<Set<ProductId>>>>
    suspend fun launchPurchasingFlow(product: ProductId): Result<Unit>
    suspend fun getProductPrice(product: ProductId): Result<String>
    suspend fun isPurchased(product: ProductId): Result<Boolean>
    fun refresh()
}
