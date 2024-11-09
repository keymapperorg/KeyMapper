package io.github.sds100.keymapper.sorting

enum class SortOrder {
    ASCENDING,
    DESCENDING,
    ;

    fun toggle(): SortOrder {
        return when (this) {
            ASCENDING -> DESCENDING
            DESCENDING -> ASCENDING
        }
    }
}
