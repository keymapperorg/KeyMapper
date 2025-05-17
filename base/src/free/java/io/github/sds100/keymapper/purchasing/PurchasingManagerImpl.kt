package io.github.sds100.keymapper.purchasing

import android.content.Context
import io.github.sds100.keymapper.base.utils.Error
import io.github.sds100.keymapper.base.utils.Result
import io.github.sds100.keymapper.base.utils.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class PurchasingManagerImpl(
    context: Context,
    private val coroutineScope: CoroutineScope,
) : PurchasingManager {
    override val onCompleteProductPurchase: MutableSharedFlow<ProductId> = MutableSharedFlow()
    override val purchases: Flow<State<Result<Set<ProductId>>>> =
        MutableStateFlow(State.Data(Error.PurchasingNotImplemented))

    override suspend fun launchPurchasingFlow(product: ProductId): Result<Unit> {
        return Error.PurchasingNotImplemented
    }

    override suspend fun getProductPrice(product: ProductId): Result<String> {
        return Error.PurchasingNotImplemented
    }

    override suspend fun isPurchased(product: ProductId): Result<Boolean> {
        return Error.PurchasingNotImplemented
    }

    override fun refresh() {}
}
