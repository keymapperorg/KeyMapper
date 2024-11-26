package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// This was supposed to be used but it became to complex. #1223

/**
 * Observes the order in which key map fields should be sorted, prioritizing specific fields.
 * For example, if the order is [TRIGGER, ACTIONS, CONSTRAINTS, OPTIONS],
 * it means the key maps should be sorted first by trigger, then by actions, followed by constraints,
 * and finally by options.
 */
class ObserveKeyMapSortOrderUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    private val default by lazy {
        listOf(
            SortField.TRIGGER,
            SortField.ACTIONS,
            SortField.CONSTRAINTS,
            SortField.OPTIONS,
        )
    }

    operator fun invoke(): Flow<List<SortField>> {
        return preferenceRepository
            .get(Keys.sortOrder)
            .map {
                val result = runCatching {
                    it
                        ?.split(",")
                        ?.map { SortField.valueOf(it) }
                        ?: default
                }.getOrDefault(default).distinct()

                // If the result is not the expected size it means that the preference is corrupted
                if (result.size != 4) {
                    return@map default
                }

                result
            }
    }
}
