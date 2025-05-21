package io.github.sds100.keymapper.purchasing

import io.github.sds100.keymapper.base.purchasing.ProductId
import io.github.sds100.keymapper.base.purchasing.PurchasingError
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class PurchasingManagerImpl : PurchasingManager {
    override val onCompleteProductPurchase: MutableSharedFlow<ProductId> = MutableSharedFlow()
    override val purchases: Flow<State<Result<Set<ProductId>>>> =
        MutableStateFlow(State.Data(PurchasingError.PurchasingNotImplemented))

    override suspend fun launchPurchasingFlow(product: ProductId): Result<Unit> {
        return PurchasingError.PurchasingNotImplemented
    }

    override suspend fun getProductPrice(product: ProductId): Result<String> {
        return PurchasingError.PurchasingNotImplemented
    }

    override suspend fun isPurchased(product: ProductId): Result<Boolean> {
        return PurchasingError.PurchasingNotImplemented
    }

    override fun refresh() {}
}
