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
        require(sortOrder.size == SortField.values().size) {
            "Sort order set must have ${SortField.values().size} fields"
        }

        preferenceRepository.set(
            Keys.sortOrder,
            sortOrder.map { it.name }.toSet(),
        )
    }
}
