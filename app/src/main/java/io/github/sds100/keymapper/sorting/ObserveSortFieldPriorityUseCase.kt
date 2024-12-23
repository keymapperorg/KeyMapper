package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes the order in which key map fields should be sorted, prioritizing specific fields.
 * For example, if the order is [TRIGGER, ACTIONS, CONSTRAINTS, OPTIONS],
 * it means the key maps should be sorted first by trigger, then by actions, followed by constraints,
 * and finally by options.
 */
class ObserveSortFieldPriorityUseCase(
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
                    val mapped = it?.split(",")?.map { SortField.valueOf(it) }

                    mapped ?: default
                }.getOrDefault(default).distinct()

                // If the result is not the expected size it means that the preference is corrupted
                // or there are missing fields (e.g. a new field was added). In this case, return
                // the default order.
                if (result.size != SortField.entries.size) {
                    return@map default
                }

                result
            }
    }
}
