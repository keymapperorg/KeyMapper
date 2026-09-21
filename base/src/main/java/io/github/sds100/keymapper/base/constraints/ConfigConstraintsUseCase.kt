package io.github.sds100.keymapper.base.constraints

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

@ViewModelScoped
class ConfigConstraintsUseCaseImpl @Inject constructor(
    private val state: ConfigKeyMapState,
    private val preferenceRepository: PreferenceRepository,
) : ConfigConstraintsUseCase {

    override val keyMap: StateFlow<State<KeyMap>> = state.keyMap

    /**
     * The most recently used is first.
     */
    override val recentlyUsedConstraints: Flow<List<ConstraintData>> =
        combine(
            preferenceRepository.get(Keys.recentlyUsedConstraints).map(::getConstraintShortcuts),
            keyMap.filterIsInstance<State.Data<KeyMap>>(),
        ) { shortcutData, keyMap ->

            // Do not include constraints that the key map already contains.
            shortcutData
                .filter { constraintData ->
                    !keyMap.data.constraintState.allConstraints.any { it.data == constraintData }
                }
                .take(5)
        }

    override fun addConstraint(
        groupUid: String?,
        constraintData: ConstraintData,
    ): ConstraintGroup? {
        var containsConstraint = false
        var affectedGroup: ConstraintGroup? = null
        val newConstraint = Constraint(data = constraintData)

        val newKeyMap = updateConstraintState { oldState ->
            val group = oldState.groups.find { it.uid == groupUid }

            if (group == null) {
                val newGroup = ConstraintGroup(constraints = listOf(newConstraint))

                // Use OR mode by default when there are multiple groups. I think this is the
                // most likely use case because if you want multiple constraints to match with AND
                // you will put them in the same group.
                val mode = if (oldState.groups.size <= 1) {
                    ConstraintMode.OR
                } else {
                    oldState.mode
                }

                affectedGroup = newGroup

                return@updateConstraintState oldState.copy(
                    groups = oldState.groups.plus(newGroup),
                    mode = mode,
                )
            }

            containsConstraint =
                group.constraints.any { it.data == constraintData && !it.isNot }

            if (containsConstraint) {
                oldState
            } else {
                oldState.updateGroup(group.uid) { group ->
                    affectedGroup = group

                    group.copy(constraints = group.constraints.plus(newConstraint))
                }
            }
        }

        if (newKeyMap == null) {
            return null
        }

        preferenceRepository.update(Keys.recentlyUsedConstraints) { old ->
            val oldDataList = getConstraintShortcuts(old)

            val newDataList = LinkedList(oldDataList)
                .apply { addFirst(constraintData) }
                .distinct()

            Json.encodeToString(newDataList)
        }

        return if (containsConstraint) {
            null
        } else {
            affectedGroup
        }
    }

    override fun removeConstraint(uid: String) {
        updateConstraintState { oldState ->
            val groups = oldState.groups
                .map { group ->
                    group.copy(constraints = group.constraints.filterNot { it.uid == uid })
                }
                .filter { it.constraints.isNotEmpty() }

            oldState.copy(groups = groups)
        }
    }

    override fun removeGroup(groupUid: String) {
        updateConstraintState { oldState ->
            oldState.copy(groups = oldState.groups.filterNot { it.uid == groupUid })
        }
    }

    override fun toggleNot(constraintUid: String) {
        updateConstraintState { oldState ->
            val groups = oldState.groups.map { group ->
                val constraints = group.constraints.map { constraint ->
                    if (constraint.uid == constraintUid) {
                        constraint.copy(isNot = !constraint.isNot)
                    } else {
                        constraint
                    }
                }

                group.copy(constraints = constraints)
            }

            oldState.copy(groups = groups)
        }
    }

    override fun setMode(mode: ConstraintMode) {
        updateConstraintState { oldState ->
            oldState.copy(mode = mode)
        }
    }

    override fun setGroupMode(groupUid: String, mode: ConstraintMode) {
        updateConstraintState { oldState ->
            oldState.updateGroup(groupUid) { it.copy(mode = mode) }
        }
    }

    override fun setGroupName(groupUid: String, name: String?) {
        updateConstraintState { oldState ->
            oldState.updateGroup(groupUid) { it.copy(name = name?.trim()?.ifBlank { null }) }
        }
    }

    override fun moveGroup(fromIndex: Int, toIndex: Int) {
        updateConstraintState { oldState ->
            oldState.copy(groups = oldState.groups.move(fromIndex, toIndex))
        }
    }

    private fun updateConstraintState(block: (ConstraintState) -> ConstraintState): KeyMap? {
        return state.update { keyMap ->
            keyMap.copy(constraintState = block(keyMap.constraintState))
        }.dataOrNull()
    }

    private fun ConstraintState.updateGroup(
        groupUid: String,
        block: (ConstraintGroup) -> ConstraintGroup,
    ): ConstraintState {
        val groups = groups.map { group ->
            if (group.uid == groupUid) {
                block(group)
            } else {
                group
            }
        }

        return copy(groups = groups)
    }

    private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in indices || toIndex !in indices) {
            return this
        }

        return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    private fun getConstraintShortcuts(json: String?): List<ConstraintData> {
        if (json == null) {
            return emptyList()
        }

        try {
            // Decode each constraint separately so constraints that no longer exist are skipped
            // without losing the other shortcuts.
            return Json.parseToJsonElement(json).jsonArray
                .mapNotNull { element ->
                    try {
                        Json.decodeFromJsonElement(ConstraintData.serializer(), element)
                    } catch (_: Exception) {
                        null
                    }
                }
                .distinct()
        } catch (_: Exception) {
            return emptyList()
        }
    }
}

interface ConfigConstraintsUseCase {
    val keyMap: StateFlow<State<KeyMap>>

    val recentlyUsedConstraints: Flow<List<ConstraintData>>

    /**
     * @param groupUid the group to add the constraint to. A new group is created if this is null.
     * @return the group the constraint was added to (a newly created group, or an existing
     * group), or null if the group already contains the constraint (a duplicate).
     */
    fun addConstraint(groupUid: String?, constraintData: ConstraintData): ConstraintGroup?
    fun removeConstraint(uid: String)
    fun removeGroup(groupUid: String)
    fun toggleNot(constraintUid: String)

    /**
     * Set the mode that combines the groups.
     */
    fun setMode(mode: ConstraintMode)
    fun setGroupMode(groupUid: String, mode: ConstraintMode)
    fun setGroupName(groupUid: String, name: String?)
    fun moveGroup(fromIndex: Int, toIndex: Int)
}
