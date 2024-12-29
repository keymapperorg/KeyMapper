package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SortKeyMapsUseCaseImpl(
    private val preferenceRepository: PreferenceRepository,
) : SortKeyMapsUseCase {
    private val defaultOrder = listOf(
        SortFieldOrder(SortField.TRIGGER),
        SortFieldOrder(SortField.ACTIONS),
        SortFieldOrder(SortField.CONSTRAINTS),
        SortFieldOrder(SortField.OPTIONS),
    )

    /**
     * Observes the order in which key map fields should be sorted, prioritizing specific fields.
     * For example, if the order is [TRIGGER, ACTIONS, CONSTRAINTS, OPTIONS],
     * it means the key maps should be sorted first by trigger, then by actions, followed by constraints,
     * and finally by options.
     */
    override fun observeSortFieldOrder(): Flow<List<SortFieldOrder>> {
        return preferenceRepository
            .get(Keys.sortOrderJson)
            .map {
                if (it == null) {
                    return@map defaultOrder
                }

                val result = runCatching {
                    Json.decodeFromString<List<SortFieldOrder>>(it)
                }.getOrDefault(defaultOrder).distinct()

                // If the result is not the expected size it means that the preference is corrupted
                // or there are missing fields (e.g. a new field was added). In this case, return
                // the default order.

                if (result.size != SortField.entries.size) {
                    return@map defaultOrder
                }

                result
            }
    }

    override fun setSortFieldOrder(sortFieldOrders: List<SortFieldOrder>) {
        val json = Json.encodeToString(sortFieldOrders)
        preferenceRepository.set(Keys.sortOrderJson, json)
    }

    override fun observeKeyMapsSorter(): Flow<Comparator<KeyMap>> {
        return observeSortFieldOrder()
            .map { list ->
                list.filter { it.order != SortOrder.NONE }
                    .map(SortFieldOrder::getComparator)
            }
            .map { Sorter(it) }
    }
}

interface SortKeyMapsUseCase {
    fun observeSortFieldOrder(): Flow<List<SortFieldOrder>>
    fun setSortFieldOrder(sortFieldOrders: List<SortFieldOrder>)
    fun observeKeyMapsSorter(): Flow<Comparator<KeyMap>>
}

private class Sorter(
    private val comparatorsOrder: List<Comparator<KeyMap>>,
) : Comparator<KeyMap> {
    override fun compare(
        keyMap: KeyMap?,
        otherKeyMap: KeyMap?,
    ): Int {
        if (keyMap == null || otherKeyMap == null) {
            return 0
        }

        // Take the result of the first comparator that returns a non-zero value.
        return comparatorsOrder
            .asSequence()
            .map { it.compare(keyMap, otherKeyMap) }
            .firstOrNull { it != 0 }
            ?: 0
    }
}
