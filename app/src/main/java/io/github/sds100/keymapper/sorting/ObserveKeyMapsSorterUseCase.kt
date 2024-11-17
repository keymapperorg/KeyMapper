package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Observes the key maps comparator based on the user's preferences.
 */
class ObserveKeyMapsSorterUseCase(
    private val observeKeyMapFieldSortOrderUseCase: ObserveKeyMapFieldSortOrderUseCase,
    private val observeKeyMapSortOrderUseCase: ObserveKeyMapSortOrderUseCase,
) {
    operator fun invoke(): Flow<Comparator<KeyMap>> {
        return combine(
            observeKeyMapFieldSortOrderUseCase(SortField.TRIGGER),
            observeKeyMapFieldSortOrderUseCase(SortField.ACTIONS),
            observeKeyMapFieldSortOrderUseCase(SortField.CONSTRAINTS),
            observeKeyMapFieldSortOrderUseCase(SortField.OPTIONS),
            observeKeyMapSortOrderUseCase(),
        ) { trigger, actions, constraints, options, order ->
            order.map {
                val fieldOrder = when (it) {
                    SortField.TRIGGER -> trigger
                    SortField.ACTIONS -> actions
                    SortField.CONSTRAINTS -> constraints
                    SortField.OPTIONS -> options
                }

                it.getComparator(fieldOrder == SortOrder.DESCENDING)
            }
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
