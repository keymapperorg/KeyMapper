package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.base.utils.TestConstraintSnapshot
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class ConstraintSnapshotTest {

    private val satisfied = Constraint(data = ConstraintData.AppInForeground("key_mapper"))
    private val satisfied2 = Constraint(data = ConstraintData.ScreenOn)
    private val unsatisfied = Constraint(data = ConstraintData.AppInForeground("google"))
    private val unsatisfied2 = Constraint(data = ConstraintData.WifiOn)

    /**
     * The app in foreground and the screen is on. Wi-Fi is off.
     */
    private val snapshot = TestConstraintSnapshot(
        appInForeground = "key_mapper",
        isScreenOn = true,
        isWifiEnabled = false,
    )

    private fun group(
        vararg constraints: Constraint,
        mode: ConstraintMode = ConstraintMode.AND,
    ): ConstraintGroup = ConstraintGroup(constraints = constraints.toList(), mode = mode)

    private fun state(
        vararg groups: ConstraintGroup,
        mode: ConstraintMode = ConstraintMode.AND,
    ): ConstraintState = ConstraintState(groups = groups.toList(), mode = mode)

    @Test
    fun `When two constraints in three states, one OR and one AND, and all satisfied return true`() {
        val snapshot = TestConstraintSnapshot(
            appInForeground = "key_mapper",
            isCharging = false,
            isLocked = false,
            isLockscreenShowing = true,
        )

        val state1 = state(
            group(
                Constraint(data = ConstraintData.AppInForeground(packageName = "key_mapper")),
                Constraint(data = ConstraintData.Charging, isNot = true),
                mode = ConstraintMode.AND,
            ),
        )

        val state2 = state(
            group(
                Constraint(data = ConstraintData.LockScreenShowing, isNot = true),
                Constraint(data = ConstraintData.DeviceIsLocked, isNot = true),
                mode = ConstraintMode.OR,
            ),
        )

        val state3 = state(
            group(
                Constraint(data = ConstraintData.LockScreenShowing),
                Constraint(data = ConstraintData.DeviceIsLocked, isNot = true),
                mode = ConstraintMode.AND,
            ),
        )

        assertThat(snapshot.isSatisfied(state1, state2, state3), `is`(true))
    }

    @Test
    fun `When two constraints in two states, one OR and one AND, and all unsatisfied return false`() {
        val snapshot = TestConstraintSnapshot(
            appInForeground = "key_mapper",
            isCharging = true,
            isLocked = true,
        )

        val state1 = state(
            group(
                Constraint(data = ConstraintData.AppInForeground(packageName = "key_mapper")),
                Constraint(data = ConstraintData.Charging, isNot = true),
                mode = ConstraintMode.AND,
            ),
        )

        val state2 = state(
            group(
                Constraint(data = ConstraintData.Charging),
                Constraint(data = ConstraintData.DeviceIsLocked, isNot = true),
                mode = ConstraintMode.OR,
            ),
        )

        assertThat(snapshot.isSatisfied(state1, state2), `is`(false))
    }

    @Test
    fun `When two constraints in two states, one OR and one AND, and all satisfied return true`() {
        val snapshot = TestConstraintSnapshot(
            appInForeground = "key_mapper",
            isCharging = true,
            isLocked = true,
        )

        val state1 = state(
            group(
                Constraint(data = ConstraintData.AppInForeground(packageName = "key_mapper")),
                Constraint(data = ConstraintData.Charging),
                mode = ConstraintMode.AND,
            ),
        )

        val state2 = state(
            group(
                Constraint(data = ConstraintData.Charging),
                Constraint(data = ConstraintData.DeviceIsLocked, isNot = true),
                mode = ConstraintMode.OR,
            ),
        )

        assertThat(snapshot.isSatisfied(state1, state2), `is`(true))
    }

    @Test
    fun `When one constraint in two states and all satisfied return true`() {
        assertThat(
            snapshot.isSatisfied(state(group(satisfied)), state(group(satisfied2))),
            `is`(true),
        )
    }

    @Test
    fun `When one constraint in two states and all unsatisfied return false`() {
        assertThat(
            snapshot.isSatisfied(state(group(unsatisfied)), state(group(unsatisfied2))),
            `is`(false),
        )
    }

    @Test
    fun `When one constraint in two states and one unsatisfied return false`() {
        assertThat(
            snapshot.isSatisfied(state(group(unsatisfied)), state(group(satisfied))),
            `is`(false),
        )
    }

    @Test
    fun `When no constraints in two states return true`() {
        assertThat(snapshot.isSatisfied(state(), state()), `is`(true))
    }

    @Test
    fun `When no constraints in two states with mixed constraint modes return true`() {
        assertThat(
            snapshot.isSatisfied(state(mode = ConstraintMode.OR), state(mode = ConstraintMode.AND)),
            `is`(true),
        )
    }

    @Test
    fun `When one constraint and unsatisfied return false`() {
        assertThat(snapshot.isSatisfied(state(group(unsatisfied))), `is`(false))
    }

    @Test
    fun `When one constraint and satisfied return true`() {
        assertThat(snapshot.isSatisfied(state(group(satisfied))), `is`(true))
    }

    @Test
    fun `When no constraints return true`() {
        assertThat(snapshot.isSatisfied(state()), `is`(true))
    }

    @Test
    fun `When keyboard is showing and constraint is KeyboardShowing return true`() {
        val snapshot = TestConstraintSnapshot(isKeyboardShowing = true)
        val constraint = Constraint(data = ConstraintData.KeyboardShowing)
        assertThat(snapshot.isSatisfied(state(group(constraint))), `is`(true))
    }

    @Test
    fun `When keyboard is not showing and constraint is KeyboardShowing return false`() {
        val snapshot = TestConstraintSnapshot(isKeyboardShowing = false)
        val constraint = Constraint(data = ConstraintData.KeyboardShowing)
        assertThat(snapshot.isSatisfied(state(group(constraint))), `is`(false))
    }

    @Test
    fun `When keyboard is not showing and constraint is NOT KeyboardShowing return true`() {
        val snapshot = TestConstraintSnapshot(isKeyboardShowing = false)
        val constraint = Constraint(data = ConstraintData.KeyboardShowing, isNot = true)
        assertThat(snapshot.isSatisfied(state(group(constraint))), `is`(true))
    }

    @Test
    fun `When keyboard is showing and constraint is NOT KeyboardShowing return false`() {
        val snapshot = TestConstraintSnapshot(isKeyboardShowing = true)
        val constraint = Constraint(data = ConstraintData.KeyboardShowing, isNot = true)
        assertThat(snapshot.isSatisfied(state(group(constraint))), `is`(false))
    }

    @Test
    fun `NOT on a satisfied constraint makes it unsatisfied`() {
        assertThat(snapshot.isSatisfied(satisfied.copy(isNot = true)), `is`(false))
    }

    @Test
    fun `NOT on an unsatisfied constraint makes it satisfied`() {
        assertThat(snapshot.isSatisfied(unsatisfied.copy(isNot = true)), `is`(true))
    }

    @Test
    fun `AND group with all satisfied constraints is satisfied`() {
        val state = state(group(satisfied, satisfied2, mode = ConstraintMode.AND))
        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `AND group with one unsatisfied constraint is not satisfied`() {
        val state = state(group(satisfied, unsatisfied, mode = ConstraintMode.AND))
        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `OR group with one satisfied constraint is satisfied`() {
        val state = state(group(unsatisfied, satisfied, mode = ConstraintMode.OR))
        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `OR group with no satisfied constraints is not satisfied`() {
        val state = state(group(unsatisfied, unsatisfied2, mode = ConstraintMode.OR))
        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `AND between groups when all groups are satisfied`() {
        val state = state(
            group(satisfied),
            group(satisfied2),
            mode = ConstraintMode.AND,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `AND between groups when one group is not satisfied`() {
        val state = state(
            group(satisfied),
            group(unsatisfied),
            mode = ConstraintMode.AND,
        )

        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `OR between groups when one group is satisfied`() {
        val state = state(
            group(unsatisfied),
            group(satisfied),
            mode = ConstraintMode.OR,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `OR between groups when no groups are satisfied`() {
        val state = state(
            group(unsatisfied),
            group(unsatisfied2),
            mode = ConstraintMode.OR,
        )

        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `(A OR B) AND (C OR D) when each group has one satisfied constraint`() {
        val state = state(
            group(unsatisfied, satisfied, mode = ConstraintMode.OR),
            group(satisfied2, unsatisfied2, mode = ConstraintMode.OR),
            mode = ConstraintMode.AND,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `(A OR B) AND (C OR D) when one group has no satisfied constraints`() {
        val state = state(
            group(unsatisfied, satisfied, mode = ConstraintMode.OR),
            group(unsatisfied, unsatisfied2, mode = ConstraintMode.OR),
            mode = ConstraintMode.AND,
        )

        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `(A AND B) OR (C AND D) when only the second group is fully satisfied`() {
        val state = state(
            group(satisfied, unsatisfied, mode = ConstraintMode.AND),
            group(satisfied, satisfied2, mode = ConstraintMode.AND),
            mode = ConstraintMode.OR,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `(A AND B) OR (C AND D) when neither group is fully satisfied`() {
        val state = state(
            group(satisfied, unsatisfied, mode = ConstraintMode.AND),
            group(unsatisfied2, satisfied2, mode = ConstraintMode.AND),
            mode = ConstraintMode.OR,
        )

        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `(A AND B) AND (C OR D) with mixed group modes`() {
        val state = state(
            group(satisfied, satisfied2, mode = ConstraintMode.AND),
            group(unsatisfied, satisfied, mode = ConstraintMode.OR),
            mode = ConstraintMode.AND,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `(A OR B) OR (C AND D) with mixed group modes when only the OR group is satisfied`() {
        val state = state(
            group(unsatisfied, satisfied, mode = ConstraintMode.OR),
            group(satisfied2, unsatisfied2, mode = ConstraintMode.AND),
            mode = ConstraintMode.OR,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `NOT constraints inside groups are inverted before combining the groups`() {
        // (NOT unsatisfied AND satisfied) AND (NOT satisfied OR NOT unsatisfied2)
        val state = state(
            group(unsatisfied.copy(isNot = true), satisfied, mode = ConstraintMode.AND),
            group(
                satisfied.copy(isNot = true),
                unsatisfied2.copy(isNot = true),
                mode = ConstraintMode.OR,
            ),
            mode = ConstraintMode.AND,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `NOT on every constraint in an OR group is not satisfied when all are satisfied`() {
        val state = state(
            group(
                satisfied.copy(isNot = true),
                satisfied2.copy(isNot = true),
                mode = ConstraintMode.OR,
            ),
        )

        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `Empty groups are ignored when combining groups with AND`() {
        val state = state(
            group(),
            group(satisfied),
            mode = ConstraintMode.AND,
        )

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `Empty groups do not satisfy an OR between groups`() {
        val state = state(
            group(),
            group(unsatisfied),
            mode = ConstraintMode.OR,
        )

        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `Only empty groups are satisfied`() {
        val state = state(group(), group(mode = ConstraintMode.OR), mode = ConstraintMode.OR)

        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `Multiple states with groups are combined with AND`() {
        val keyMapState = state(
            group(unsatisfied),
            group(satisfied),
            mode = ConstraintMode.OR,
        )

        val satisfiedGroupState = state(group(satisfied, satisfied2))
        val unsatisfiedGroupState =
            state(group(unsatisfied, unsatisfied2, mode = ConstraintMode.OR))

        assertThat(snapshot.isSatisfied(keyMapState, satisfiedGroupState), `is`(true))
        assertThat(snapshot.isSatisfied(keyMapState, unsatisfiedGroupState), `is`(false))
    }
}
