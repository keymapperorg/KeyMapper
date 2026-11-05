package io.github.sds100.keymapper.base.sorting

import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionUiHelper
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapActionsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapConstraintsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapEnabledComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapNameComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapOptionsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapTriggerComparator
import io.github.sds100.keymapper.base.trigger.TriggerUiHelper
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SortKeyMapsUseCaseImpl @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    displayKeyMapUseCase: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider,
) : SortKeyMapsUseCase {
    private val actionUiHelper = ActionUiHelper(displayKeyMapUseCase, resourceProvider)
    private val constraintUiHelper = ConstraintUiHelper(displayKeyMapUseCase, resourceProvider)
    private val triggerUiHelper = TriggerUiHelper(resourceProvider)

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
                }.getOrDefault(defaultOrder).distinctBy { sortFieldOrder -> sortFieldOrder.field }

                // There can be missing fields if a new field was added since the preference was
                // saved. Add them in their default position at the start so the user's existing
                // order is kept. They have no sort order so this doesn't change the sorting.
                defaultOrder.filter { default ->
                    result.none { sortFieldOrder -> sortFieldOrder.field == default.field }
                } + result
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
            SortField.TRIGGER -> KeyMapTriggerComparator(
                { triggerUiHelper.getKeyNames(it, showDeviceDescriptors = false) },
                reverseOrder,
            )

            // Titles are cached because they can include an app name, which is slow to look up
            // and would otherwise be fetched on every comparison.
            SortField.ACTIONS -> {
                val titles = HashMap<Action, String>()

                KeyMapActionsComparator(
                    { titles.getOrPut(it) { actionUiHelper.getTitle(it, false) } },
                    reverseOrder,
                )
            }

            SortField.CONSTRAINTS -> {
                val titles = HashMap<Constraint, String>()

                KeyMapConstraintsComparator(
                    { titles.getOrPut(it) { constraintUiHelper.getTitle(it) } },
                    reverseOrder,
                )
            }

            SortField.OPTIONS -> KeyMapOptionsComparator(reverseOrder)
            SortField.ENABLED -> KeyMapEnabledComparator(reverseOrder)
            SortField.NAME -> KeyMapNameComparator(reverseOrder)
        }
    }

    companion object {
        val defaultOrder = listOf(
            SortFieldOrder(SortField.NAME),
            SortFieldOrder(SortField.TRIGGER),
            SortFieldOrder(SortField.ACTIONS),
            SortFieldOrder(SortField.CONSTRAINTS),
            SortFieldOrder(SortField.OPTIONS),
            SortFieldOrder(SortField.ENABLED),
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

private class Sorter(private val comparatorsOrder: List<Comparator<KeyMap>>) : Comparator<KeyMap> {
    override fun compare(keyMap: KeyMap?, otherKeyMap: KeyMap?): Int {
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
