package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.util.TestConstraintSnapshot
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class ConstraintSnapshotTest {
    @Test
    fun `When two constraints in three states, one OR and one AND, and all satisfied return true`() {
        val snapshot = TestConstraintSnapshot(
            appInForeground = "key_mapper",
            isCharging = false,
            isLocked = false,
            isLockscreenShowing = true,
        )

        val state1 = ConstraintState(
            constraints =
            setOf(
                Constraint.AppInForeground(packageName = "key_mapper"),
                Constraint.Discharging(),
            ),
            mode = ConstraintMode.AND,
        )

        val state2 =
            ConstraintState(
                constraints =
                setOf(
                    Constraint.LockScreenNotShowing(),
                    Constraint.DeviceIsUnlocked(),
                ),
                mode = ConstraintMode.OR,
            )

        val state3 =
            ConstraintState(
                constraints =
                setOf(
                    Constraint.LockScreenShowing(),
                    Constraint.DeviceIsUnlocked(),
                ),
                mode = ConstraintMode.AND,
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

        val state1 = ConstraintState(
            constraints =
            setOf(
                Constraint.AppInForeground(packageName = "key_mapper"),
                Constraint.Discharging(),
            ),
            mode = ConstraintMode.AND,
        )

        val state2 =
            ConstraintState(
                constraints =
                setOf(
                    Constraint.Charging(),
                    Constraint.DeviceIsUnlocked(),
                ),
                mode = ConstraintMode.OR,
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

        val state1 = ConstraintState(
            constraints =
            setOf(
                Constraint.AppInForeground(packageName = "key_mapper"),
                Constraint.Charging(),
            ),
            mode = ConstraintMode.AND,
        )

        val state2 =
            ConstraintState(
                constraints =
                setOf(
                    Constraint.Charging(),
                    Constraint.DeviceIsUnlocked(),
                ),
                mode = ConstraintMode.OR,
            )

        assertThat(snapshot.isSatisfied(state1, state2), `is`(true))
    }

    @Test
    fun `When one constraint in two states and all satisfied return true`() {
        val snapshot = TestConstraintSnapshot(appInForeground = "key_mapper", isCharging = true)

        val state1 = ConstraintState(
            constraints =
            setOf(Constraint.AppInForeground(packageName = "key_mapper")),
        )

        val state2 =
            ConstraintState(
                constraints =
                setOf(Constraint.Charging()),
            )

        assertThat(snapshot.isSatisfied(state1, state2), `is`(true))
    }

    @Test
    fun `When one constraint in two states and all unsatisfied return false`() {
        val snapshot = TestConstraintSnapshot(appInForeground = "key_mapper")

        val state1 = ConstraintState(
            constraints =
            setOf(Constraint.AppInForeground(packageName = "google")),
        )

        val state2 =
            ConstraintState(
                constraints =
                setOf(Constraint.AppInForeground(packageName = "google1")),
            )

        assertThat(snapshot.isSatisfied(state1, state2), `is`(false))
    }

    @Test
    fun `When one constraint in two states and one unsatisfied return false`() {
        val snapshot = TestConstraintSnapshot(appInForeground = "key_mapper")

        val state1 = ConstraintState(
            constraints =
            setOf(Constraint.AppInForeground(packageName = "google")),
        )

        val state2 =
            ConstraintState(
                constraints =
                setOf(Constraint.AppInForeground(packageName = "key_mapper")),
            )

        assertThat(snapshot.isSatisfied(state1, state2), `is`(false))
    }

    @Test
    fun `When no constraints in two states return true`() {
        val snapshot = TestConstraintSnapshot()

        val state1 = ConstraintState(constraints = emptySet())
        val state2 = ConstraintState(constraints = emptySet())

        assertThat(snapshot.isSatisfied(state1, state2), `is`(true))
    }

    @Test
    fun `When no constraints in two states with mixed constraint modes return true`() {
        val snapshot = TestConstraintSnapshot()

        val state1 = ConstraintState(constraints = emptySet(), mode = ConstraintMode.OR)
        val state2 = ConstraintState(constraints = emptySet(), mode = ConstraintMode.AND)

        assertThat(snapshot.isSatisfied(state1, state2), `is`(true))
    }

    @Test
    fun `When one constraint and unsatisfied return false`() {
        val snapshot = TestConstraintSnapshot(appInForeground = "key_mapper")
        val constraint = Constraint.AppInForeground(packageName = "google")
        val state = ConstraintState(constraints = setOf(constraint))
        assertThat(snapshot.isSatisfied(state), `is`(false))
    }

    @Test
    fun `When one constraint and satisfied return true`() {
        val snapshot = TestConstraintSnapshot(appInForeground = "key_mapper")
        val constraint = Constraint.AppInForeground(packageName = "key_mapper")
        val state = ConstraintState(constraints = setOf(constraint))
        assertThat(snapshot.isSatisfied(state), `is`(true))
    }

    @Test
    fun `When no constraints return true`() {
        val snapshot = TestConstraintSnapshot(appInForeground = "key_mapper")
        val state = ConstraintState(constraints = emptySet())
        assertThat(snapshot.isSatisfied(state), `is`(true))
    }
}
