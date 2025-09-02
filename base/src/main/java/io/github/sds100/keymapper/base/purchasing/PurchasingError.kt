package io.github.sds100.keymapper.base.purchasing

import io.github.sds100.keymapper.common.utils.KMError

sealed class PurchasingError : KMError() {
    data object PurchasingNotImplemented : PurchasingError()

    data class ProductNotPurchased(val product: ProductId) : PurchasingError()

    sealed class PurchasingProcessError : PurchasingError() {
        data object ProductNotFound : PurchasingProcessError()
        data object Cancelled : PurchasingProcessError()
        data object StoreProblem : PurchasingProcessError()
        data object NetworkError : PurchasingProcessError()
        data object PaymentPending : PurchasingProcessError()
        data object PurchaseInvalid : PurchasingProcessError()
        data class Unexpected(val message: String) : PurchasingProcessError()
    }
}
