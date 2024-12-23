package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository

class SetSortFieldPriorityUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    /**
     * @param sortOrder The order in which key map fields should be sorted, requiring all
     * [SortField] values to be present exactly once.
     */
    operator fun invoke(sortOrder: Set<SortField>) {
        require(sortOrder.size == SortField.entries.size) {
            "Sort order set must have ${SortField.entries.size} fields"
        }

        val mapped = sortOrder.joinToString(",") { it.name }

        preferenceRepository.set(Keys.sortOrder, mapped)
    }
}
