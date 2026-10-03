package io.github.sds100.keymapper.base.variables

import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.ActionDataEntityMapper
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.ConstraintEntityMapper
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.VariableRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

interface VariablesUseCase {
    /**
     * The variables that any key map already refers to, so that the user can pick one instead of
     * typing the name again. Nothing extra is persisted to build this.
     */
    val suggestions: Flow<List<VariableSuggestion>>

    /**
     * Only for observing when a variable changes. Use [getValues] to read them.
     */
    val values: Flow<Map<String, Long>>

    fun getValues(): Map<String, Long>

    fun setValue(name: String, value: Long): KMResult<Long>

    /**
     * Reads the variable, does the operation, then stores the result. A variable that has never
     * been set counts as 0.
     */
    fun modify(name: String, operation: VariableOperation, value: Long): KMResult<Long>

    fun reset(name: String)
    fun resetAll()
}

@Singleton
class VariablesUseCaseImpl @Inject constructor(
    private val keyMapRepository: KeyMapRepository,
    private val variableRepository: VariableRepository,
    private val configKeyMapState: ConfigKeyMapState,
) : VariablesUseCase {

    override val values: Flow<Map<String, Long>> = variableRepository.values

    override val suggestions: Flow<List<VariableSuggestion>> =
        combine(
            namesInKeyMaps(),
            namesInConfiguredKeyMap(),
            variableRepository.values,
        ) { savedNames, configuredNames, values ->
            savedNames.plus(configuredNames)
                .distinct()
                .sorted()
                .map { name -> VariableSuggestion(name, values[name] ?: 0L) }
        }.flowOn(Dispatchers.IO)

    override fun getValues(): Map<String, Long> {
        return variableRepository.values.value
    }

    override fun setValue(name: String, value: Long): KMResult<Long> {
        variableRepository.set(name, value)

        return Success(value)
    }

    override fun modify(name: String, operation: VariableOperation, value: Long): KMResult<Long> {
        val current = variableRepository.values.value[name] ?: 0L

        val newValue = try {
            when (operation) {
                // Adding and subtracting overflow silently so they must be done with the
                // methods that throw instead.
                VariableOperation.ADD -> Math.addExact(current, value)

                VariableOperation.SUBTRACT -> Math.subtractExact(current, value)
            }
        } catch (e: ArithmeticException) {
            return KMError.NumberOverflow
        }

        variableRepository.set(name, newValue)

        return Success(newValue)
    }

    override fun reset(name: String) {
        variableRepository.delete(name)
    }

    override fun resetAll() {
        variableRepository.deleteAll()
    }

    /**
     * The key map being configured may not have been saved yet so it isn't in the repository.
     */
    private fun namesInConfiguredKeyMap(): Flow<List<String>> {
        return configKeyMapState.keyMap.map { state ->
            val keyMap = state.dataOrNull() ?: return@map emptyList()

            val actionNames = keyMap.actionList
                .map { it.data }
                .mapNotNull { it.variableNameOrNull() }

            val constraintNames = keyMap.constraintState.allConstraints
                .map { it.data }
                .filterIsInstance<ConstraintData.Variable>()
                .map { it.name }

            actionNames.plus(constraintNames)
        }
    }

    private fun namesInKeyMaps(): Flow<List<String>> {
        return keyMapRepository.getAll().map { keyMapList ->
            keyMapList
                .flatMap { keyMap ->
                    val actionNames = keyMap.actionList
                        .filterNotNull()
                        .mapNotNull { ActionDataEntityMapper.fromEntity(it) }
                        .mapNotNull { it.variableNameOrNull() }

                    val constraintNames = keyMap.constraintList
                        .map { ConstraintEntityMapper.fromEntity(it).data }
                        .filterIsInstance<ConstraintData.Variable>()
                        .map { it.name }

                    actionNames.plus(constraintNames)
                }
                .distinct()
                .sorted()
        }
    }
}

private fun ActionData.variableNameOrNull(): String? {
    return when (this) {
        is ActionData.SetVariable -> name
        is ActionData.ModifyVariable -> name
        else -> null
    }
}
