package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.variables.VariableOperation
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

class ActionDataEntityMapperTest {

    @Test
    fun `set variable action round trips through the entity`() {
        val action = ActionData.SetVariable(name = "counter", value = -12)

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_VARIABLE_NAME).valueOrNull(),
            `is`("counter"),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `modify variable action round trips through the entity`() {
        val action = ActionData.ModifyVariable(
            name = "counter",
            operation = VariableOperation.SUBTRACT,
            value = -12,
        )

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(entity.type, `is`(ActionEntity.Type.MODIFY_VARIABLE))
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `modify variable action with the set operation is dropped`() {
        val entity = ActionEntity(
            type = ActionEntity.Type.MODIFY_VARIABLE,
            data = "",
            extras = listOf(
                EntityExtra(ActionEntity.EXTRA_VARIABLE_NAME, "counter"),
                EntityExtra(ActionEntity.EXTRA_VARIABLE_OPERATION, "SET"),
                EntityExtra(ActionEntity.EXTRA_VARIABLE_VALUE, "2"),
            ),
        )

        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(nullValue()))
    }

    @Test
    fun `set variable action with a value that is not a number is dropped`() {
        // A backup file can be edited by hand
        val entity = ActionEntity(
            type = ActionEntity.Type.SET_VARIABLE,
            data = "",
            extras = listOf(
                EntityExtra(ActionEntity.EXTRA_VARIABLE_NAME, "counter"),
                EntityExtra(ActionEntity.EXTRA_VARIABLE_VALUE, "not a number"),
            ),
        )

        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(nullValue()))
    }

    @Test
    fun `modify variable action with an unknown operation is dropped`() {
        val entity = ActionEntity(
            type = ActionEntity.Type.MODIFY_VARIABLE,
            data = "",
            extras = listOf(
                EntityExtra(ActionEntity.EXTRA_VARIABLE_NAME, "counter"),
                EntityExtra(ActionEntity.EXTRA_VARIABLE_OPERATION, "MULTIPLY"),
                EntityExtra(ActionEntity.EXTRA_VARIABLE_VALUE, "2"),
            ),
        )

        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(nullValue()))
    }

    @Test
    fun `open app action with no app name extra is loaded with a null saved app name`() {
        val entity = ActionEntity(
            type = ActionEntity.Type.APP,
            data = "com.example",
        )

        assertThat(
            ActionDataEntityMapper.fromEntity(entity),
            `is`(ActionData.App(packageName = "com.example", savedAppName = null)),
        )
    }

    @Test
    fun `open app action with a saved app name round trips through the entity`() {
        val action = ActionData.App(packageName = "com.example", savedAppName = "Example")

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_APP_NAME).valueOrNull(),
            `is`("Example"),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `app shortcut action with a saved app name round trips through the entity`() {
        val action = ActionData.AppShortcut(
            packageName = "com.example",
            shortcutTitle = "Do the thing",
            uri = "intent:...",
            savedAppName = "Example",
        )

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_APP_NAME).valueOrNull(),
            `is`("Example"),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `control media for app action with a saved app name round trips through the entity`() {
        val action = ActionData.ControlMediaForApp.Pause(
            packageName = "com.example",
            savedAppName = "Example",
        )

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_APP_NAME).valueOrNull(),
            `is`("Example"),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

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

    @Test
    fun `increase brightness action with a non-default step round trips through the entity`() {
        val action = ActionData.Brightness.Increase(stepPercent = 25)

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_BRIGHTNESS_STEP_PERCENT).valueOrNull(),
            `is`("25"),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `dont save an extra when the increase brightness action has no custom step`() {
        val action = ActionData.Brightness.Increase(stepPercent = null)

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_BRIGHTNESS_STEP_PERCENT).valueOrNull(),
            `is`(nullValue()),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `load no brightness step for an increase brightness action saved before it existed`() {
        // GIVEN an entity saved by an older version of the app, with no extra at all.
        val entity =
            ActionEntity(type = ActionEntity.Type.SYSTEM_ACTION, data = "increase_brightness")

        val action = ActionDataEntityMapper.fromEntity(entity)

        assertThat((action as ActionData.Brightness.Increase).stepPercent, `is`(nullValue()))
    }

    @Test
    fun `decrease brightness action with a non-default step round trips through the entity`() {
        val action = ActionData.Brightness.Decrease(stepPercent = 25)

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_BRIGHTNESS_STEP_PERCENT).valueOrNull(),
            `is`("25"),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `dont save an extra when the decrease brightness action has no custom step`() {
        val action = ActionData.Brightness.Decrease(stepPercent = null)

        val entity = ActionDataEntityMapper.toEntity(action)

        assertThat(
            entity.extras.getData(ActionEntity.EXTRA_BRIGHTNESS_STEP_PERCENT).valueOrNull(),
            `is`(nullValue()),
        )
        assertThat(ActionDataEntityMapper.fromEntity(entity), `is`(action))
    }

    @Test
    fun `load no brightness step for a decrease brightness action saved before it existed`() {
        // GIVEN an entity saved by an older version of the app, with no extra at all.
        val entity =
            ActionEntity(type = ActionEntity.Type.SYSTEM_ACTION, data = "decrease_brightness")

        val action = ActionDataEntityMapper.fromEntity(entity)

        assertThat((action as ActionData.Brightness.Decrease).stepPercent, `is`(nullValue()))
    }
}
