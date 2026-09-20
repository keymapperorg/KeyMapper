package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.vibration.VibrateEffect
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class VibrateActionEntityMapperTest {

    @Test
    fun `save and load a vibrate action with a custom duration`() {
        val action = ActionData.Vibrate(VibrateEffect.CustomDuration(500L))

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `save and load a vibrate action with a predefined effect`() {
        val action = ActionData.Vibrate(
            VibrateEffect.Predefined(VibrateEffect.PredefinedType.DOUBLE_CLICK),
        )

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }
}
