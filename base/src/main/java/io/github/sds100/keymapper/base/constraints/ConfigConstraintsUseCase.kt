package io.github.sds100.keymapper.base.constraints

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.LinkedList
import javax.inject.Inject

@ViewModelScoped
class ConfigConstraintsUseCaseImpl @Inject constructor(
    private val state: ConfigKeyMapState,
    private val preferenceRepository: PreferenceRepository
) : ConfigConstraintsUseCase {

    override val keyMap: StateFlow<State<KeyMap>> = state.keyMap

    /**
     * The most recently used is first.
     */
    override val recentlyUsedConstraints: Flow<List<Constraint>> =
        combine(
            preferenceRepository.get(Keys.recentlyUsedConstraints).map(::getConstraintShortcuts),
            keyMap.filterIsInstance<State.Data<KeyMap>>(),
        ) { shortcuts, keyMap ->

            // Do not include constraints that the key map already contains.
            shortcuts
                .filter { !keyMap.data.constraintState.constraints.contains(it) }
                .take(5)
        }

    override fun addConstraint(constraint: Constraint): Boolean {
        var containsConstraint = false

        updateConstraintState { oldState ->
            containsConstraint = oldState.constraints.contains(constraint)
            oldState.copy(constraints = oldState.constraints.plus(constraint))
        }

        preferenceRepository.update(
            Keys.recentlyUsedConstraints,
            { old ->
                val oldList: List<Constraint> = if (old == null) {
                    emptyList()
                } else {
                    Json.decodeFromString<List<Constraint>>(old)
                }

                val newShortcuts = LinkedList(oldList)
                    .also { it.addFirst(constraint) }
                    .distinct()

                Json.encodeToString(newShortcuts)
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


    private suspend fun getConstraintShortcuts(json: String?): List<Constraint> {
        if (json == null) {
            return emptyList()
        }

        try {
            return withContext(Dispatchers.Default) {
                val list = Json.decodeFromString<List<Constraint>>(json)

                list.distinct()
            }
        } catch (_: Exception) {
            preferenceRepository.set(Keys.recentlyUsedConstraints, null)
            return emptyList()
        }
    }

}

interface ConfigConstraintsUseCase {
    val keyMap: StateFlow<State<KeyMap>>

    val recentlyUsedConstraints: Flow<List<Constraint>>
    fun addConstraint(constraint: Constraint): Boolean
    fun removeConstraint(id: String)
    fun setAndMode()
    fun setOrMode()
}