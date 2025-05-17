package io.github.sds100.keymapper.keymaps

import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.groups.Group
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapModel
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapsUseCaseImpl
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Test

class ProcessKeyMapGroupsForDetectionTest {

    @Test
    fun `Key map in grandchild group, all have constraints, and parent does not exist then ignore key map`() {
        val keyMap = KeyMap(groupUid = "child")
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group(
                    "child",
                    parentUid = "parent",
                    mode = ConstraintMode.OR,
                    Constraint.LockScreenNotShowing(),
                    Constraint.Discharging(),
                ),
                group(
                    "parent",
                    parentUid = "bad_parent",
                    mode = ConstraintMode.AND,
                    Constraint.DeviceIsLocked(),
                    Constraint.NotInPhoneCall(),
                ),
            ),
        )

        assertThat(models, Matchers.empty())
    }

    @Test
    fun `Key map in grandchild group and all groups have constraints`() {
        val keyMap = KeyMap(groupUid = "child")

        val constraints1 = arrayOf(
            Constraint.LockScreenNotShowing(),
            Constraint.Discharging(),
        )

        val constraints2 = arrayOf(
            Constraint.DeviceIsLocked(),
            Constraint.NotInPhoneCall(),
        )

        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group(
                    "child",
                    parentUid = "parent",
                    mode = ConstraintMode.OR,
                    *constraints1,
                ),
                group(
                    "parent",
                    parentUid = null,
                    mode = ConstraintMode.AND,
                    *constraints2,
                ),
            ),
        )

        val expected = DetectKeyMapModel(
            keyMap,
            groupConstraintStates = listOf(
                ConstraintState(
                    constraints = constraints1.toSet(),
                    mode = ConstraintMode.OR,
                ),
                ConstraintState(
                    constraints = constraints2.toSet(),
                    mode = ConstraintMode.AND,
                ),
            ),
        )
        assertThat(models, Matchers.contains(expected))
    }

    @Test
    fun `Key map in grandchild group and child only has constraints`() {
        val keyMap = KeyMap(groupUid = "child")
        val constraints1 = arrayOf(
            Constraint.LockScreenNotShowing(),
            Constraint.Discharging(),
        )
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group(
                    "child",
                    parentUid = "parent",
                    mode = ConstraintMode.OR,
                    *constraints1,
                ),
                group(
                    "parent",
                    parentUid = null,
                ),
            ),
        )

        val expected = DetectKeyMapModel(
            keyMap,
            groupConstraintStates = listOf(
                ConstraintState(
                    constraints = constraints1.toSet(),
                    mode = ConstraintMode.OR,
                ),
            ),
        )
        assertThat(models, Matchers.contains(expected))
    }

    @Test
    fun `Key map in grandchild group and parent only has constraints`() {
        val keyMap = KeyMap(groupUid = "child")
        val constraints1 = arrayOf(
            Constraint.LockScreenNotShowing(),
            Constraint.Discharging(),
        )

        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group("child", parentUid = "parent"),
                group(
                    "parent",
                    parentUid = null,
                    mode = ConstraintMode.OR,
                    *constraints1,
                ),
            ),
        )

        val expected = DetectKeyMapModel(
            keyMap,
            groupConstraintStates = listOf(
                ConstraintState(
                    constraints = constraints1.toSet(),
                    mode = ConstraintMode.OR,
                ),
            ),
        )
        assertThat(models, Matchers.contains(expected))
    }

    @Test
    fun `Key map in grandchild group and parent exists then include`() {
        val keyMap = KeyMap(groupUid = "child")
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group("child", parentUid = "parent"),
                group("parent", parentUid = null),
            ),
        )

        assertThat(
            models,
            Matchers.contains(
                DetectKeyMapModel(keyMap = keyMap),
            ),
        )
    }

    @Test
    fun `Key maps in child and root groups then include both`() {
        val keyMap1 = KeyMap(groupUid = "child")
        val keyMap2 = KeyMap(groupUid = null)
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap1, keyMap2),
            groups = listOf(
                group("child", parentUid = null),
            ),
        )

        assertThat(
            models,
            Matchers.contains(
                DetectKeyMapModel(
                    keyMap = keyMap1,
                ),
                DetectKeyMapModel(
                    keyMap = keyMap2,
                ),
            ),
        )
    }

    @Test
    fun `One key map in child group and parent is missing then ignore key map`() {
        val keyMap = KeyMap(groupUid = "child")
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group("child", parentUid = "bad_parent"),
            ),
        )

        assertThat(models, Matchers.empty())
    }

    @Test
    fun `One key map in child group then include`() {
        val keyMap = KeyMap(groupUid = "child")
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group("child", parentUid = null),
            ),
        )

        assertThat(
            models,
            Matchers.contains(
                DetectKeyMapModel(keyMap = keyMap),
            ),
        )
    }

    @Test
    fun `Do not include empty constraint states from groups`() {
        val keyMap = KeyMap(groupUid = "group1")
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group("group1"),
            ),
        )

        assertThat(models, Matchers.contains(DetectKeyMapModel(keyMap)))
    }

    @Test
    fun `One key map in root group`() {
        val keyMap = KeyMap()
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = listOf(
                group("group1"),
            ),
        )

        assertThat(models, Matchers.contains(DetectKeyMapModel(keyMap)))
    }

    @Test
    fun `empty key maps and one group`() {
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = emptyList(),
            groups = listOf(
                group("group1"),
            ),
        )

        assertThat(models, Matchers.empty())
    }

    @Test
    fun `empty key maps`() {
        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = emptyList(),
            groups = emptyList(),
        )

        assertThat(models, Matchers.empty())
    }

    private fun group(
        uid: String,
        parentUid: String? = null,
        mode: ConstraintMode = ConstraintMode.AND,
        vararg constraint: Constraint,
    ): Group {
        return Group(
            uid = uid,
            name = uid,
            constraintState = ConstraintState(
                constraints = constraint.toSet(),
                mode = mode,
            ),
            parentUid = parentUid,
            lastOpenedDate = 0,
        )
    }
}
