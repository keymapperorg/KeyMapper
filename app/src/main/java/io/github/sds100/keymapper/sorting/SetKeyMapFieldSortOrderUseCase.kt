package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository

class SetKeyMapFieldSortOrderUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    operator fun invoke(field: SortField, order: SortOrder) {
        val key = when (field) {
            SortField.TRIGGER -> Keys.sortTriggerAscending
            SortField.ACTIONS -> Keys.sortActionsAscending
            SortField.CONSTRAINTS -> Keys.sortConstraintsAscending
            SortField.OPTIONS -> Keys.sortOptionsAscending
        }

        preferenceRepository.set(key, order.mapOrder())
    }

    private fun SortOrder.mapOrder() = when (this) {
        SortOrder.NONE -> null
        SortOrder.ASCENDING -> true
        SortOrder.DESCENDING -> false
    }
}
