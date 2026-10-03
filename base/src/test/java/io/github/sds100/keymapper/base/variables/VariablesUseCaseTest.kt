package io.github.sds100.keymapper.base.variables

import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.ActionDataEntityMapper
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.ConstraintEntityMapper
import io.github.sds100.keymapper.base.constraints.ConstraintGroup
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.VariableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class VariablesUseCaseTest {

    private lateinit var variableRepository: FakeVariableRepository
    private lateinit var keyMapRepository: FakeKeyMapRepository
    private lateinit var configKeyMap: MutableStateFlow<State<KeyMap>>
    private lateinit var useCase: VariablesUseCase

    @Before
    fun init() {
        variableRepository = FakeVariableRepository()
        keyMapRepository = FakeKeyMapRepository()
        configKeyMap = MutableStateFlow(State.Loading)
        val configKeyMapState = mock<ConfigKeyMapState> {
            on { keyMap } doReturn configKeyMap
        }
        useCase = VariablesUseCaseImpl(keyMapRepository, variableRepository, configKeyMapState)
    }

    @Test
    fun `suggestions include variables in the key map being configured before it is saved`() =
        runTest {
            configKeyMap.value = State.Data(
                KeyMap(
                    actionList = listOf(Action(data = setVariable("counter"))),
                    constraintState = ConstraintState(
                        groups = listOf(
                            ConstraintGroup(
                                constraints = listOf(Constraint(data = variableConstraint("mode"))),
                            ),
                        ),
                    ),
                ),
            )

            assertThat(
                useCase.suggestions.first(),
                `is`(listOf(VariableSuggestion("counter", 0), VariableSuggestion("mode", 0))),
            )
        }

    @Test
    fun `a variable that has never been set counts as zero`() {
        assertThat(useCase.modify("counter", VariableOperation.ADD, 1).valueOrNull(), `is`(1L))
    }

    @Test
    fun `adding and subtracting change the stored value`() {
        useCase.setValue("counter", 10)
        useCase.modify("counter", VariableOperation.ADD, 5)
        useCase.modify("counter", VariableOperation.SUBTRACT, 3)

        assertThat(useCase.getValues()["counter"], `is`(12L))
    }

    @Test
    fun `setting replaces the value rather than combining with it`() {
        useCase.setValue("mode", 7)
        useCase.setValue("mode", 2)

        assertThat(useCase.getValues()["mode"], `is`(2L))
    }

    @Test
    fun `adding past the maximum is an error and leaves the value alone`() {
        useCase.setValue("counter", Long.MAX_VALUE)

        val result = useCase.modify("counter", VariableOperation.ADD, 1)

        assertThat(result, `is`(KMError.NumberOverflow))
        assertThat(useCase.getValues()["counter"], `is`(Long.MAX_VALUE))
    }

    @Test
    fun `subtracting past the minimum is an error and leaves the value alone`() {
        useCase.setValue("counter", Long.MIN_VALUE)

        val result = useCase.modify("counter", VariableOperation.SUBTRACT, 1)

        assertThat(result, `is`(KMError.NumberOverflow))
        assertThat(useCase.getValues()["counter"], `is`(Long.MIN_VALUE))
    }

    @Test
    fun `resetting a variable makes it read as zero again`() {
        useCase.setValue("counter", 9)

        useCase.reset("counter")

        assertThat(useCase.getValues()["counter"], `is`(nullValue()))
        assertThat(useCase.modify("counter", VariableOperation.ADD, 1), `is`(Success(1L)))
    }

    @Test
    fun `resetting everything clears a variable that no key map refers to`() {
        useCase.setValue("orphan", 5)

        useCase.resetAll()

        assertThat(useCase.getValues().isEmpty(), `is`(true))
    }

    @Test
    fun `suggestions include names from both actions and constraints`() = runTest {
        keyMapRepository.keyMaps.value = listOf(
            keyMapEntity(actions = listOf(setVariable("counter"))),
            keyMapEntity(constraints = listOf(variableConstraint("mode"))),
        )

        useCase.setValue("counter", 3)

        assertThat(
            useCase.suggestions.first(),
            `is`(listOf(VariableSuggestion("counter", 3), VariableSuggestion("mode", 0))),
        )
    }

    @Test
    fun `a variable no key map refers to is not suggested but is still stored`() = runTest {
        useCase.setValue("orphan", 5)

        assertThat(useCase.suggestions.first().isEmpty(), `is`(true))
        assertThat(useCase.getValues()["orphan"], `is`(5L))
    }

    @Test
    fun `the same name used by many key maps is only suggested once`() = runTest {
        keyMapRepository.keyMaps.value = listOf(
            keyMapEntity(actions = listOf(setVariable("counter"))),
            keyMapEntity(actions = listOf(setVariable("counter"))),
        )

        assertThat(useCase.suggestions.first().size, `is`(1))
    }

    private fun setVariable(name: String): ActionData {
        return ActionData.SetVariable(name, 1)
    }

    private fun variableConstraint(name: String): ConstraintData {
        return ConstraintData.Variable(name, VariableComparison.EQUALS, 2)
    }

    private fun keyMapEntity(
        actions: List<ActionData> = emptyList(),
        constraints: List<ConstraintData> = emptyList(),
    ): KeyMapEntity {
        return KeyMapEntity(
            id = 0,
            actionList = actions.map { ActionDataEntityMapper.toEntity(it) },
            constraintList = constraints.map {
                ConstraintEntityMapper.toEntity(Constraint(data = it))
            },
        )
    }
}

private class FakeVariableRepository : VariableRepository {
    override val values = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun set(name: String, value: Long) {
        values.value = values.value.plus(name to value)
    }

    override fun delete(name: String) {
        values.value = values.value.minus(name)
    }

    override fun deleteAll() {
        values.value = emptyMap()
    }
}

private class FakeKeyMapRepository : KeyMapRepository {
    val keyMaps = MutableStateFlow<List<KeyMapEntity>>(emptyList())

    override val keyMapList: Flow<State<List<KeyMapEntity>>>
        get() = throw NotImplementedError()

    override fun getAll(): Flow<List<KeyMapEntity>> = keyMaps
    override fun getByGroup(groupUid: String?): Flow<List<KeyMapEntity>> = keyMaps
    override fun insert(vararg keyMap: KeyMapEntity) = Unit
    override fun update(vararg keyMap: KeyMapEntity) = Unit
    override suspend fun get(uid: String): KeyMapEntity? = null
    override fun delete(vararg uid: String) = Unit
    override suspend fun deleteAll() = Unit
    override fun count(): Flow<Int> = throw NotImplementedError()
    override fun duplicate(vararg uid: String) = Unit
    override fun enableById(vararg uid: String) = Unit
    override fun disableById(vararg uid: String) = Unit
    override fun enableByGroup(groupUid: String?) = Unit
    override fun disableByGroup(groupUid: String?) = Unit
    override fun toggleByGroup(groupUid: String?) = Unit
    override fun toggleById(vararg uid: String) = Unit
    override fun moveToGroup(groupUid: String?, vararg uid: String) = Unit
}
