package io.github.sds100.keymapper.base.sorting

import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapActionsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapConstraintsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapOptionsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapTriggerComparator
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SortKeyMapsUseCaseImpl @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val displayKeyMapUseCase: DisplayKeyMapUseCase,
) : SortKeyMapsUseCase {

    override val showHelp = preferenceRepository.get(Keys.sortShowHelp).map { it ?: true }

    override fun setShowHelp(show: Boolean) {
        preferenceRepository.set(Keys.sortShowHelp, show)
    }

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
                    .map(::getComparator)
            }
            .map { Sorter(it) }
    }

    private fun getComparator(sortFieldOrder: SortFieldOrder): Comparator<KeyMap> {
        val reverseOrder = sortFieldOrder.order == SortOrder.DESCENDING

        return when (sortFieldOrder.field) {
            SortField.TRIGGER -> KeyMapTriggerComparator(reverseOrder)
            SortField.ACTIONS -> KeyMapActionsComparator(displayKeyMapUseCase, reverseOrder)
            SortField.CONSTRAINTS -> KeyMapConstraintsComparator(
                displayKeyMapUseCase,
                reverseOrder,
            )

            SortField.OPTIONS -> KeyMapOptionsComparator(reverseOrder)
        }
    }

    companion object {
        val defaultOrder = listOf(
            SortFieldOrder(SortField.TRIGGER),
            SortFieldOrder(SortField.ACTIONS),
            SortFieldOrder(SortField.CONSTRAINTS),
            SortFieldOrder(SortField.OPTIONS),
        )
    }
}

interface SortKeyMapsUseCase {
    val showHelp: Flow<Boolean>
    fun setShowHelp(show: Boolean)

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
