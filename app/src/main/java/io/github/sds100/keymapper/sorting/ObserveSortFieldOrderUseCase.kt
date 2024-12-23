package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Observes the order in which key map fields should be sorted, prioritizing specific fields.
 * For example, if the order is [TRIGGER, ACTIONS, CONSTRAINTS, OPTIONS],
 * it means the key maps should be sorted first by trigger, then by actions, followed by constraints,
 * and finally by options.
 */
class ObserveSortFieldOrderUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    private val default by lazy {
        listOf(
            SortFieldOrder(SortField.TRIGGER),
            SortFieldOrder(SortField.ACTIONS),
            SortFieldOrder(SortField.CONSTRAINTS),
            SortFieldOrder(SortField.OPTIONS),
        )
    }

    operator fun invoke(): Flow<List<SortFieldOrder>> {
        return preferenceRepository
            .get(Keys.sortOrderJson)
            .map {
                if (it == null) {
                    return@map default
                }

                val result = runCatching {
                    Json.Default.decodeFromString<List<SortFieldOrder>>(it)
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
