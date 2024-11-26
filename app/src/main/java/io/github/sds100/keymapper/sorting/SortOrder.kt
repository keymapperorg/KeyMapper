package io.github.sds100.keymapper.sorting

enum class SortOrder {
    NONE,
    ASCENDING,
    DESCENDING,
    ;

    fun toggle(): SortOrder {
        return when (this) {
            NONE -> ASCENDING
            ASCENDING -> DESCENDING
            DESCENDING -> NONE
        }
    }
}
