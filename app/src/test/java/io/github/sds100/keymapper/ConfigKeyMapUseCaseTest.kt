package io.github.sds100.keymapper

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.singleKeyTrigger
import io.github.sds100.keymapper.util.triggerKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Created by sds100 on 19/04/2021.
 */

@ExperimentalCoroutinesApi
class ConfigKeyMapUseCaseTest {
    private lateinit var useCase: ConfigKeyMapUseCaseImpl

    @Before
    fun init() {
        useCase = ConfigKeyMapUseCaseImpl(
            devicesAdapter = mock(),
            keyMapRepository = mock(),
            preferenceRepository = mock()
        )
    }

    /**
     * Issue #753. If a modifier key is used as a trigger then it the
     * option to not override the default action must be chosen so that the modifier
     * key can still be used normally.
     */
    @Test
    fun `when add modifier key trigger, enable do not remap option`() =
        runTest {
            val modifierKeys = setOf(
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.KEYCODE_SHIFT_RIGHT,
                KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.KEYCODE_ALT_RIGHT,
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.KEYCODE_CTRL_RIGHT,
                KeyEvent.KEYCODE_META_LEFT,
                KeyEvent.KEYCODE_META_RIGHT,
                KeyEvent.KEYCODE_SYM,
                KeyEvent.KEYCODE_NUM,
                KeyEvent.KEYCODE_FUNCTION
            )

            for (modifierKeyCode in modifierKeys) {
                //GIVEN
                useCase.mapping.value = State.Data(KeyMap())

                //WHEN
                useCase.addTriggerKey(modifierKeyCode, TriggerKeyDevice.Internal)

                //THEN
                val trigger = useCase.mapping.value.dataOrNull()!!.trigger

                assertThat(trigger.keys[0].consumeKeyEvent, `is`(false))
            }
        }

    /**
     * Issue #753.
     */
    @Test
    fun `when add non-modifier key trigger, do ont enable do not remap option`() =
        runTest {
            //GIVEN
            useCase.mapping.value = State.Data(KeyMap())

            //WHEN
            useCase.addTriggerKey(KeyEvent.KEYCODE_A, TriggerKeyDevice.Internal)

            //THEN
            val trigger = useCase.mapping.value.dataOrNull()!!.trigger

            assertThat(trigger.keys[0].consumeKeyEvent, `is`(true))
        }

    /**
     * Issue #852. Add a phone ringing constraint when you add an action
     * to answer a phone call.
     */
    @Test
    fun `when add answer phone call action, then add phone ringing constraint`() =
        runTest {
            //GIVEN
            useCase.mapping.value = State.Data(KeyMap())
            val action = ActionData.AnswerCall

            //WHEN
            useCase.addAction(action)

            //THEN
            val keyMap = useCase.mapping.value.dataOrNull()!!
            assertThat(keyMap.constraintState.constraints, contains(Constraint.PhoneRinging))
        }

    /**
     * Issue #852. Add a in phone call constraint when you add an action
     * to end a phone call.
     */
    @Test
    fun `when add end phone call action, then add in phone call constraint`() =
        runTest {
            //GIVEN
            useCase.mapping.value = State.Data(KeyMap())
            val action = ActionData.EndCall

            //WHEN
            useCase.addAction(action)

            //THEN
            val keyMap = useCase.mapping.value.dataOrNull()!!
            assertThat(keyMap.constraintState.constraints, contains(Constraint.InPhoneCall))
        }

    /**
     * issue #593
     */
    @Test
    fun `key map with hold down action, load key map, hold down flag shouldn't disappear`() =
        runTest {
            //given
            val action = KeyMapAction(
                data = ActionData.TapScreen(100, 100, null),
                holdDown = true
            )

            val keyMap = KeyMap(
                0,
                trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_0)),
                actionList = listOf(action)
            )

            //when
            useCase.mapping.value = State.Data(keyMap)

            //then
            assertThat(useCase.mapping.value.dataOrNull()!!.actionList, `is`(listOf(action)))
        }

    @Test
    fun `add modifier key event action, enable hold down option and disable repeat option`() =
        runTest {
            KeyEventUtils.MODIFIER_KEYCODES.forEach { keyCode ->
                useCase.mapping.value = State.Data(KeyMap())

                useCase.addAction(ActionData.InputKeyEvent(keyCode))

                useCase.mapping.value.dataOrNull()!!.actionList
                    .single()
                    .let {
                        assertThat(it.holdDown, `is`(true))
                        assertThat(it.repeat, `is`(false))
                    }
            }
        }
}