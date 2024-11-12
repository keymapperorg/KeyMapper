package io.github.sds100.keymapper.purchasing

import android.content.Context
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.CoroutineScope

class PurchasingManagerImpl(
    context: Context,
    private val coroutineScope: CoroutineScope,
) : PurchasingManager {
    override suspend fun launchPurchasingFlow(product: ProductId): Result<Unit> {
        return Error.PurchasingNotImplemented
    }

    override suspend fun getProductPrice(product: ProductId): Result<String> {
        return Error.PurchasingNotImplemented
    }

    override suspend fun isPurchased(product: ProductId): Result<Boolean> {
        return Error.PurchasingNotImplemented
    }
}
