package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.data.entities.ActionEntity
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

    @Test
    fun `action is enabled when the disabled flag is not set`() {
        val entity =
            ActionEntity(type = ActionEntity.Type.SYSTEM_ACTION, data = "consume_key_event")

        assertThat(ActionEntityMapper.fromEntity(entity)?.isEnabled, `is`(true))
    }

    @Test
    fun `save and restore a disabled action`() {
        val action = Action(data = ActionData.ConsumeKeyEvent, isEnabled = false)

        val entity = ActionEntityMapper.toEntity(KeyMap(actionList = listOf(action))).single()

        assertThat(entity.flags.hasFlag(ActionEntity.ACTION_FLAG_DISABLED), `is`(true))
        assertThat(ActionEntityMapper.fromEntity(entity)?.isEnabled, `is`(false))
    }

    @Test
    fun `do not set the disabled flag for an enabled action`() {
        val action = Action(data = ActionData.ConsumeKeyEvent, isEnabled = true)

        val entity = ActionEntityMapper.toEntity(KeyMap(actionList = listOf(action))).single()

        assertThat(entity.flags.hasFlag(ActionEntity.ACTION_FLAG_DISABLED), `is`(false))
    }
}
