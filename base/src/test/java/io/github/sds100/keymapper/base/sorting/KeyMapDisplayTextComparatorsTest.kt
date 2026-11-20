package io.github.sds100.keymapper.base.sorting

import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.ConstraintGroup
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapActionsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapConstraintsComparator
import io.github.sds100.keymapper.base.sorting.comparators.KeyMapTriggerComparator
import io.github.sds100.keymapper.base.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.base.trigger.AssistantTriggerType
import io.github.sds100.keymapper.base.trigger.Trigger
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class KeyMapDisplayTextComparatorsTest {

    @Test
    fun `sort actions alphabetically by title instead of action type`() {
        val titles = mapOf(
            ActionData.Url("url1") to "Banana",
            ActionData.Text("text") to "apple",
            ActionData.Url("url2") to "Cherry",
        )
        val keyMaps = titles.keys.map { KeyMap(actionList = listOf(Action(data = it))) }

        fun sorted(reverse: Boolean): List<String?> {
            val comparator = KeyMapActionsComparator({ titles.getValue(it.data) }, reverse)

            return keyMaps.sortedWith(comparator).map { titles[it.actionList[0].data] }
        }

        assertThat(sorted(reverse = false), `is`(listOf("apple", "Banana", "Cherry")))
        assertThat(sorted(reverse = true), `is`(listOf("Cherry", "Banana", "apple")))
    }

    @Test
    fun `sort constraints alphabetically by title instead of constraint type`() {
        val titles = mapOf(
            ConstraintData.ScreenOn to "Banana",
            ConstraintData.MediaPlaying to "apple",
            ConstraintData.Charging to "Cherry",
        )
        val keyMaps = titles.keys.map { data ->
            KeyMap(
                constraintState = ConstraintState(
                    groups = listOf(ConstraintGroup(constraints = listOf(Constraint(data = data)))),
                ),
            )
        }

        val comparator = KeyMapConstraintsComparator({ titles.getValue(it.data) })
        val sorted = keyMaps.sortedWith(comparator)
            .map { titles[it.constraintState.allConstraints[0].data] }

        assertThat(sorted, `is`(listOf("apple", "Banana", "Cherry")))
    }

    @Test
    fun `sort triggers alphabetically by key name instead of key type`() {
        val names = mapOf(
            AssistantTriggerType.ANY to "Banana",
            AssistantTriggerType.VOICE to "apple",
            AssistantTriggerType.DEVICE to "Cherry",
        )
        val keyMaps = names.keys.map { type ->
            KeyMap(
                trigger = Trigger(
                    keys = listOf(
                        AssistantTriggerKey(type = type, clickType = ClickType.SHORT_PRESS),
                    ),
                ),
            )
        }

        val comparator = KeyMapTriggerComparator(
            { trigger ->
                trigger.keys.map { names.getValue((it as AssistantTriggerKey).type) }
            },
        )
        val sorted = keyMaps.sortedWith(comparator)
            .map { names[(it.trigger.keys[0] as AssistantTriggerKey).type] }

        assertThat(sorted, `is`(listOf("apple", "Banana", "Cherry")))
    }
}
