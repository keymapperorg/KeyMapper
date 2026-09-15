package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.keymaps.KeyMap
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test

class ActionEntityMapperTest {

    @Test
    fun `save and restore the custom name of an action`() {
        val action = Action(data = ActionData.ConsumeKeyEvent, customName = "My action")

        val entity = ActionEntityMapper.toEntity(KeyMap(actionList = listOf(action))).single()

        assertThat(ActionEntityMapper.fromEntity(entity)?.customName, `is`("My action"))
    }

    @Test
    fun `do not save a blank custom name`() {
        val action = Action(data = ActionData.ConsumeKeyEvent, customName = " ")

        val entity = ActionEntityMapper.toEntity(KeyMap(actionList = listOf(action))).single()

        assertThat(ActionEntityMapper.fromEntity(entity)?.customName, nullValue())
    }
}
