package io.github.sds100.keymapper.base.sorting

import kotlinx.serialization.Serializable

@Serializable
data class SortFieldOrder(
    val field: SortField,
    val order: SortOrder = SortOrder.NONE,
)
