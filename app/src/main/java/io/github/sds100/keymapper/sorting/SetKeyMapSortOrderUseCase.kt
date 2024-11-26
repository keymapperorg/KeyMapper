package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository

// This was supposed to be used but it became to complex. #1223

class SetKeyMapSortOrderUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    /**
     * @param sortOrder The order in which key map fields should be sorted, requiring all 4 fields.
     */
    operator fun invoke(sortOrder: List<SortField>) {
        val sortOrder = sortOrder.distinct()

        // Illegal sort order
        if (sortOrder.size != 4) {
            throw IllegalArgumentException("Sort order must have 4 unique fields")
        }

        preferenceRepository.set(
            Keys.sortOrder,
            sortOrder.joinToString(",") { it.name },
        )
    }
}
