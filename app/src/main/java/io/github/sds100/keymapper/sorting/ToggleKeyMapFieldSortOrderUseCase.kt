package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapField
import io.github.sds100.keymapper.util.firstBlocking

class ToggleKeyMapFieldSortOrderUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    operator fun invoke(field: KeyMapField) {
        val key = when (field) {
            KeyMapField.TRIGGER -> Keys.sortTriggerAscending
            KeyMapField.ACTIONS -> Keys.sortActionsAscending
            KeyMapField.CONSTRAINTS -> Keys.sortConstraintsAscending
            KeyMapField.OPTIONS -> Keys.sortOptionsAscending
        }

        preferenceRepository.get(key).firstBlocking().let {
            // If the value is null it is most likely that the user has never changed
            // the sort order before and default value is ascending (see ObserveKeyMapFieldSortOrderUseCase),
            // so we set it to descending.
            // Otherwise just toggle the value
            val next = when (it) {
                null -> false
                else -> !it
            }

            preferenceRepository.set(key, next)
        }
    }
}
