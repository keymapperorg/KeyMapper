package io.github.sds100.keymapper.base.constraints

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.LinkedList
import javax.inject.Inject

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
                    !keyMap.data.constraintState.constraints.any { it.data == constraintData }
                }
                .take(5)
        }

    override fun addConstraint(constraintData: ConstraintData): Boolean {
        var containsConstraint = false
        val newConstraint = Constraint(data = constraintData)

        updateConstraintState { oldState ->
            containsConstraint = oldState.constraints.any { it.data == constraintData }

            if (containsConstraint) {
                oldState
            } else {
                oldState.copy(constraints = oldState.constraints.plus(newConstraint))
            }
        }

        preferenceRepository.update(
            Keys.recentlyUsedConstraints,
            { old ->
                val oldDataList = getConstraintShortcuts(old)

                val newDataList = LinkedList(oldDataList)
                    .apply { addFirst(constraintData) }
                    .distinct()

                Json.encodeToString(newDataList)
            },
        )

        return !containsConstraint
    }

    override fun removeConstraint(id: String) {
        updateConstraintState { oldState ->
            val newList = oldState.constraints.toMutableSet().apply {
                removeAll { it.uid == id }
            }
            oldState.copy(constraints = newList)
        }
    }

    override fun setAndMode() {
        updateConstraintState { oldState ->
            oldState.copy(mode = ConstraintMode.AND)
        }
    }

    override fun setOrMode() {
        updateConstraintState { oldState ->
            oldState.copy(mode = ConstraintMode.OR)
        }
    }

    private fun updateConstraintState(block: (ConstraintState) -> ConstraintState) {
        state.update { keyMap ->
            keyMap.copy(constraintState = block(keyMap.constraintState))
        }
    }

    private fun getConstraintShortcuts(json: String?): List<ConstraintData> {
        if (json == null) {
            return emptyList()
        }

        try {
            return Json.decodeFromString<List<ConstraintData>>(json).distinct()
        } catch (_: Exception) {
            return emptyList()
        }
    }
}

interface ConfigConstraintsUseCase {
    val keyMap: StateFlow<State<KeyMap>>

    val recentlyUsedConstraints: Flow<List<ConstraintData>>
    fun addConstraint(constraintData: ConstraintData): Boolean
    fun removeConstraint(id: String)
    fun setAndMode()
    fun setOrMode()
}
