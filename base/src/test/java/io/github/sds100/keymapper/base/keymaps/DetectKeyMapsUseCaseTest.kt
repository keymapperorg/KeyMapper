package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.ConstraintGroup
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.detection.DetectKeyMapModel
import io.github.sds100.keymapper.base.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.groups.Group
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Test

class DetectKeyMapsUseCaseTest {

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
                    Constraint(data = ConstraintData.LockScreenShowing, isNot = true),
                    Constraint(data = ConstraintData.Charging, isNot = true),
                ),
                group(
                    "parent",
                    parentUid = "bad_parent",
                    mode = ConstraintMode.AND,
                    Constraint(data = ConstraintData.DeviceIsLocked),
                    Constraint(data = ConstraintData.NotInPhoneCall),
                ),
            ),
        )

        assertThat(models, Matchers.empty())
    }

    @Test
    fun `Key map in grandchild group and all groups have constraints`() {
        val keyMap = KeyMap(groupUid = "child")

        val constraints1 = arrayOf(
            Constraint(data = ConstraintData.LockScreenShowing, isNot = true),
            Constraint(data = ConstraintData.Charging, isNot = true),
        )

        val constraints2 = arrayOf(
            Constraint(data = ConstraintData.DeviceIsLocked),
            Constraint(data = ConstraintData.NotInPhoneCall),
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
                constraintState(ConstraintMode.OR, *constraints1),
                constraintState(ConstraintMode.AND, *constraints2),
            ),
        )
        assertThat(models, Matchers.contains(expected))
    }

    @Test
    fun `Key map in grandchild group and child only has constraints`() {
        val keyMap = KeyMap(groupUid = "child")
        val constraints1 = arrayOf(
            Constraint(data = ConstraintData.LockScreenShowing, isNot = true),
            Constraint(data = ConstraintData.Charging, isNot = true),
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
                constraintState(ConstraintMode.OR, *constraints1),
            ),
        )
        assertThat(models, Matchers.contains(expected))
    }

    @Test
    fun `Key map in grandchild group and parent only has constraints`() {
        val keyMap = KeyMap(groupUid = "child")
        val constraints1 = arrayOf(
            Constraint(data = ConstraintData.LockScreenShowing, isNot = true),
            Constraint(data = ConstraintData.Charging, isNot = true),
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
                constraintState(ConstraintMode.OR, *constraints1),
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
    fun `Key map has disabled actions then remove the disabled actions`() {
        val enabledAction = Action(data = ActionData.GoHome)
        val disabledAction = Action(data = ActionData.ConsumeKeyEvent, isEnabled = false)
        val keyMap = KeyMap(actionList = listOf(enabledAction, disabledAction))

        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = emptyList(),
        )

        assertThat(models.single().keyMap.actionList, Matchers.contains(enabledAction))
    }

    @Test
    fun `Key map has only disabled actions then action list is empty`() {
        val disabledAction = Action(data = ActionData.ConsumeKeyEvent, isEnabled = false)
        val keyMap = KeyMap(actionList = listOf(disabledAction))

        val models = DetectKeyMapsUseCaseImpl.processKeyMapsAndGroups(
            keyMaps = listOf(keyMap),
            groups = emptyList(),
        )

        assertThat(models.single().keyMap.actionList, Matchers.empty())
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
            constraintState = constraintState(mode, *constraint),
            parentUid = parentUid,
            lastOpenedDate = 0,
        )
    }

    private fun constraintState(
        mode: ConstraintMode,
        vararg constraints: Constraint,
    ): ConstraintState {
        val groups = if (constraints.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ConstraintGroup(uid = "group", constraints = constraints.toList(), mode = mode),
            )
        }

        return ConstraintState(groups = groups, mode = mode)
    }
}
