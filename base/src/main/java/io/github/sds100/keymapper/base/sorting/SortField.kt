package io.github.sds100.keymapper.base.sorting

import kotlinx.serialization.Serializable

@Serializable
enum class SortField {
    TRIGGER,
    ACTIONS,
    CONSTRAINTS,
    OPTIONS,
}
