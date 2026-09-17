package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.ConstraintGroupEntity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test

class ConstraintStateEntityMapperTest {

    @Test
    fun `flat constraints are loaded as one group with the saved mode`() {
        val entities = listOf(
            ConstraintEntity("uid1", ConstraintEntity.SCREEN_ON),
            ConstraintEntity("uid2", ConstraintEntity.CHARGING),
        )

        val state = ConstraintStateEntityMapper.fromEntity(entities, ConstraintEntity.MODE_OR)

        assertThat(state.mode, `is`(ConstraintMode.OR))
        assertThat(state.groups, hasSize(1))
        assertThat(state.groups[0].mode, `is`(ConstraintMode.OR))
        assertThat(
            state.groups[0].constraints,
            contains(
                Constraint(uid = "uid1", data = ConstraintData.ScreenOn),
                Constraint(uid = "uid2", data = ConstraintData.Charging),
            ),
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated constraints are migrated in one group`() {
        val entities = listOf(
            ConstraintEntity("uid1", ConstraintEntity.SCREEN_OFF),
            ConstraintEntity("uid2", ConstraintEntity.DISCHARGING),
        )

        val state = ConstraintStateEntityMapper.fromEntity(entities, ConstraintEntity.MODE_AND)

        assertThat(state.groups.size, `is`(1))
        assertThat(
            state.constraints,
            `is`(
                listOf(
                    Constraint(uid = "uid1", data = ConstraintData.ScreenOn, isNot = true),
                    Constraint(uid = "uid2", data = ConstraintData.Charging, isNot = true),
                ),
            ),
        )
    }

    @Test
    fun `no constraints are loaded as no groups`() {
        val state = ConstraintStateEntityMapper.fromEntity(emptyList(), ConstraintEntity.MODE_OR)

        assertThat(state.groups, empty())
        assertThat(state.mode, `is`(ConstraintMode.OR))
    }

    @Test
    fun `save and load flat constraints keeps the constraints and mode`() {
        val state = ConstraintState(
            groups = listOf(
                ConstraintGroup(
                    uid = "uid1",
                    constraints = listOf(
                        Constraint(uid = "uid1", data = ConstraintData.ScreenOn, isNot = true),
                        Constraint(uid = "uid2", data = ConstraintData.Charging),
                    ),
                    mode = ConstraintMode.OR,
                ),
            ),
            mode = ConstraintMode.OR,
        )

        val (entities, mode) = ConstraintStateEntityMapper.toEntity(state)

        assertThat(entities.map { it.groupUid }, everyItem(nullValue()))

        val loadedState = ConstraintStateEntityMapper.fromEntity(entities, mode)

        assertThat(loadedState.mode, `is`(ConstraintMode.OR))
        assertThat(loadedState.groups, hasSize(1))
        assertThat(loadedState.groups[0].mode, `is`(ConstraintMode.OR))
        assertThat(
            state.groups[0].constraints,
            contains(
                Constraint(uid = "uid1", data = ConstraintData.ScreenOn, isNot = true),
                Constraint(uid = "uid2", data = ConstraintData.Charging),
            ),
        )
    }

    @Test
    fun `no groups are saved with the constraint state mode`() {
        val state = ConstraintState(groups = emptyList(), mode = ConstraintMode.OR)

        val (entities, mode) = ConstraintStateEntityMapper.toEntity(state)

        assertThat(entities, empty())
        assertThat(mode, `is`(ConstraintEntity.MODE_OR))
    }

    @Test
    fun `constraints saved before groups existed are loaded as one group with the saved mode`() {
        val entities = listOf(
            ConstraintEntity("uid1", ConstraintEntity.SCREEN_ON),
            ConstraintEntity("uid2", ConstraintEntity.CHARGING),
        )

        val state = ConstraintStateEntityMapper.fromEntityWithGroups(
            entities,
            emptyList(),
            ConstraintEntity.MODE_OR,
        )

        assertThat(state.mode, `is`(ConstraintMode.OR))
        assertThat(state.groups, hasSize(1))
        assertThat(state.groups[0].mode, `is`(ConstraintMode.OR))
        assertThat(
            state.groups[0].constraints,
            contains(
                Constraint(uid = "uid1", data = ConstraintData.ScreenOn),
                Constraint(uid = "uid2", data = ConstraintData.Charging),
            ),
        )
    }

    @Test
    fun `no constraints with groups are loaded as no groups`() {
        val state = ConstraintStateEntityMapper.fromEntityWithGroups(
            emptyList(),
            emptyList(),
            ConstraintEntity.MODE_OR,
        )

        assertThat(state.groups, empty())
        assertThat(state.mode, `is`(ConstraintMode.OR))
    }

    @Test
    fun `no groups are saved with groups as the constraint state mode`() {
        val state = ConstraintState(groups = emptyList(), mode = ConstraintMode.OR)

        val (entities, groups, mode) = ConstraintStateEntityMapper.toEntityWithGroups(state)

        assertThat(entities, empty())
        assertThat(groups, empty())
        assertThat(mode, `is`(ConstraintEntity.MODE_OR))
    }

    @Test
    fun `one group is saved with a group entity and with the group mode`() {
        val state = ConstraintState(
            groups = listOf(
                ConstraintGroup(
                    uid = "group1",
                    constraints = listOf(
                        Constraint(uid = "uid1", data = ConstraintData.ScreenOn),
                        Constraint(uid = "uid2", data = ConstraintData.Charging, isNot = true),
                    ),
                    mode = ConstraintMode.OR,
                ),
            ),
            mode = ConstraintMode.AND,
        )

        val (entities, groups, groupMode) = ConstraintStateEntityMapper.toEntityWithGroups(state)

        assertThat(groupMode, `is`(ConstraintEntity.MODE_AND))
        assertThat(groups, hasSize(1))
        assertThat(
            groups,
            contains(ConstraintGroupEntity(uid = "group1", mode = ConstraintEntity.MODE_OR)),
        )
        assertThat(entities.map { it.groupUid }, everyItem(`is`("group1")))
    }

    @Test
    fun `multiple groups are saved with a group entity per group and the mode between groups`() {
        val state = ConstraintState(
            groups = listOf(
                ConstraintGroup(
                    uid = "group1",
                    name = "My group",
                    constraints = listOf(Constraint(uid = "uid1", data = ConstraintData.ScreenOn)),
                    mode = ConstraintMode.OR,
                ),
                ConstraintGroup(
                    uid = "group2",
                    constraints = listOf(Constraint(uid = "uid2", data = ConstraintData.Charging)),
                    mode = ConstraintMode.AND,
                ),
            ),
            mode = ConstraintMode.OR,
        )

        val (entities, groups, mode) = ConstraintStateEntityMapper.toEntityWithGroups(state)

        assertThat(mode, `is`(ConstraintEntity.MODE_OR))
        assertThat(entities.map { it.groupUid }, `is`(listOf("group1", "group2")))
        assertThat(
            groups,
            `is`(
                listOf(
                    ConstraintGroupEntity(
                        uid = "group1",
                        name = "My group",
                        mode = ConstraintEntity.MODE_OR,
                    ),
                    ConstraintGroupEntity(
                        uid = "group2",
                        name = null,
                        mode = ConstraintEntity.MODE_AND,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `save and load multiple named groups keeps the constraints, names, modes and order`() {
        val state = ConstraintState(
            groups = listOf(
                ConstraintGroup(
                    uid = "group1",
                    name = "Screen state",
                    constraints = listOf(
                        Constraint(uid = "uid1", data = ConstraintData.ScreenOn),
                        Constraint(
                            uid = "uid2",
                            data = ConstraintData.AppInForeground("com.example"),
                            isNot = true,
                        ),
                    ),
                    mode = ConstraintMode.AND,
                ),
                ConstraintGroup(
                    uid = "group2",
                    constraints = listOf(
                        Constraint(uid = "uid3", data = ConstraintData.WifiOn, isNot = true),
                    ),
                    mode = ConstraintMode.OR,
                ),
                ConstraintGroup(
                    uid = "group3",
                    name = "Charging stuff",
                    constraints = listOf(
                        Constraint(uid = "uid4", data = ConstraintData.Charging),
                        Constraint(uid = "uid5", data = ConstraintData.KeyboardShowing),
                    ),
                    mode = ConstraintMode.OR,
                ),
            ),
            mode = ConstraintMode.OR,
        )

        val (entities, groups, mode) = ConstraintStateEntityMapper.toEntityWithGroups(state)
        val loadedState = ConstraintStateEntityMapper.fromEntityWithGroups(entities, groups, mode)

        assertThat(loadedState, `is`(state))
    }

    @Test
    fun `save and load one group keeps the constraints and mode`() {
        val state = ConstraintState(
            groups = listOf(
                ConstraintGroup(
                    uid = "uid1",
                    constraints = listOf(
                        Constraint(uid = "uid1", data = ConstraintData.ScreenOn, isNot = true),
                        Constraint(uid = "uid2", data = ConstraintData.Charging),
                    ),
                    mode = ConstraintMode.OR,
                ),
            ),
            mode = ConstraintMode.OR,
        )

        val (entities, groups, mode) = ConstraintStateEntityMapper.toEntityWithGroups(state)
        val loadedState = ConstraintStateEntityMapper.fromEntityWithGroups(entities, groups, mode)

        assertThat(loadedState, `is`(state))
    }

    @Test
    fun `constraints without a group uid are put in their own group when other constraints have groups`() {
        val entities = listOf(
            ConstraintEntity(
                type = ConstraintEntity.SCREEN_ON,
                extras = emptyList(),
                uid = "uid1",
                groupUid = "group1",
            ),
            ConstraintEntity("uid2", ConstraintEntity.CHARGING),
            ConstraintEntity("uid3", ConstraintEntity.WIFI_ON),
        )

        val groupEntities = listOf(
            ConstraintGroupEntity(uid = "group1", mode = ConstraintEntity.MODE_OR),
        )

        val state = ConstraintStateEntityMapper.fromEntityWithGroups(
            entities,
            groupEntities,
            ConstraintEntity.MODE_AND,
        )

        val expected = ConstraintState(
            groups = listOf(
                ConstraintGroup(
                    uid = "group1",
                    constraints = listOf(Constraint(uid = "uid1", data = ConstraintData.ScreenOn)),
                    mode = ConstraintMode.OR,
                ),
                ConstraintGroup(
                    uid = "uid2",
                    constraints = listOf(
                        Constraint(uid = "uid2", data = ConstraintData.Charging),
                        Constraint(uid = "uid3", data = ConstraintData.WifiOn),
                    ),
                    mode = ConstraintMode.AND,
                ),
            ),
            mode = ConstraintMode.AND,
        )

        assertThat(state, `is`(expected))
    }

    @Test
    fun `group without a matching group entity uses the AND mode and no name`() {
        val entities = listOf(
            ConstraintEntity(
                type = ConstraintEntity.SCREEN_ON,
                extras = emptyList(),
                uid = "uid1",
                groupUid = "group1",
            ),
            ConstraintEntity(
                type = ConstraintEntity.CHARGING,
                extras = emptyList(),
                uid = "uid2",
                groupUid = "group2",
            ),
        )

        val state = ConstraintStateEntityMapper.fromEntityWithGroups(
            entities,
            emptyList(),
            ConstraintEntity.MODE_OR,
        )

        assertThat(state.groups.map { it.mode }, everyItem(`is`(ConstraintMode.AND)))
        assertThat(state.groups.map { it.name }, everyItem(nullValue()))
        assertThat(state.mode, `is`(ConstraintMode.OR))
    }
}
