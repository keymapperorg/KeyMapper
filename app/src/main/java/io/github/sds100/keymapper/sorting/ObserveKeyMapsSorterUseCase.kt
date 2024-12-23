package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes the key maps comparator based on the user's preferences.
 */
class ObserveKeyMapsSorterUseCase(
    private val observeSortFieldOrderUseCase: ObserveSortFieldOrderUseCase,
) {
    operator fun invoke(): Flow<Comparator<KeyMap>> {
        return observeSortFieldOrderUseCase().map { list ->
            list
                .filter { it.order != SortOrder.NONE }
                .map(SortFieldOrder::getComparator)
        }.map { Sorter(it) }
    }
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
