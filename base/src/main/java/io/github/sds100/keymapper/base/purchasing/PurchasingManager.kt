package io.github.sds100.keymapper.base.purchasing

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface PurchasingManager {
    val onCompleteProductPurchase: MutableSharedFlow<ProductId>
    val purchases: Flow<State<KMResult<Set<ProductId>>>>
    suspend fun launchPurchasingFlow(product: ProductId): KMResult<Unit>
    suspend fun getProductPrice(product: ProductId): KMResult<String>
    suspend fun isPurchased(product: ProductId): KMResult<Boolean>
    fun refresh()
}
