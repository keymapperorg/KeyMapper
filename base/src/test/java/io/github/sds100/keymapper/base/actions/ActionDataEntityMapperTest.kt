package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.utils.PinchScreenType
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.getData
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test

/**
 * Tests for saving the screen resolution with the coordinate actions in issue #2217.
 */
class ActionDataEntityMapperTest {

    @Test
    fun `save and load the screen resolution of a tap screen action`() {
        // GIVEN
        val action = ActionData.TapScreen(
            x = 540,
            y = 1200,
            description = "test",
            screenResolution = SizeKM(1080, 2400),
        )

        // WHEN
        val entity = ActionDataEntityMapper.toEntity(action)

        // THEN
        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_SCREEN_RESOLUTION).valueOrNull(),
            `is`("1080,2400"),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `save and load the screen resolution of a swipe screen action`() {
        // GIVEN
        val action = ActionData.SwipeScreen(
            xStart = 270,
            yStart = 600,
            xEnd = 540,
            yEnd = 1200,
            fingerCount = 1,
            duration = 250,
            description = "test",
            screenResolution = SizeKM(1080, 2400),
        )

        // WHEN
        val entity = ActionDataEntityMapper.toEntity(action)

        // THEN
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `save and load the screen resolution of a pinch screen action`() {
        // GIVEN
        val action = ActionData.PinchScreen(
            x = 540,
            y = 1200,
            distance = 300,
            pinchType = PinchScreenType.PINCH_IN,
            fingerCount = 2,
            duration = 250,
            description = "test",
            screenResolution = SizeKM(1080, 2400),
        )

        // WHEN
        val entity = ActionDataEntityMapper.toEntity(action)

        // THEN
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `dont save an extra when the action has no screen resolution`() {
        // GIVEN
        val action = ActionData.TapScreen(x = 540, y = 1200, description = null)

        // WHEN
        val entity = ActionDataEntityMapper.toEntity(action)

        // THEN
        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_SCREEN_RESOLUTION).valueOrNull(),
            `is`(nullValue()),
        )
    }

    @Test
    fun `load no screen resolution for an action saved before it existed`() {
        // GIVEN an entity saved by an older version of the app.
        val entity = ActionEntity(type = ActionEntity.Type.TAP_COORDINATE, data = "540,1200")

        // WHEN
        val action = ActionDataEntityMapper.fromEntity(entity)

        // THEN
        assertThat((action as ActionData.TapScreen).screenResolution, `is`(nullValue()))
    }

    @Test
    fun `load no screen resolution when the extra can not be parsed`() {
        // GIVEN
        val malformedValues = listOf("abc", "1080", "1080,2400,3", "1080,abc", "0,2400", "-1,2400")

        for (malformedValue in malformedValues) {
            val entity = ActionEntity(
                type = ActionEntity.Type.TAP_COORDINATE,
                data = "540,1200",
                extras = listOf(
                    EntityExtra(ActionEntity.EXTRA_SCREEN_RESOLUTION, malformedValue),
                ),
            )

            // WHEN
            val action = ActionDataEntityMapper.fromEntity(entity)

            // THEN the action still loads rather than throwing.
            assertThat(malformedValue, (action as ActionData.TapScreen).x, `is`(540))
            assertThat(malformedValue, action.screenResolution, `is`(nullValue()))
        }
    }
}
