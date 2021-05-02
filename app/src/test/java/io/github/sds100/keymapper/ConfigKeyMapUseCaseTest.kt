package io.github.sds100.keymapper

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.KeyEventAction
import io.github.sds100.keymapper.actions.TapCoordinateAction
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.singleKeyTrigger
import io.github.sds100.keymapper.util.triggerKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Created by sds100 on 19/04/2021.
 */

@ExperimentalCoroutinesApi
class ConfigKeyMapUseCaseTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var useCase: ConfigKeyMapUseCaseImpl

    @Before
    fun init() {
        useCase = ConfigKeyMapUseCaseImpl(
            devicesAdapter = mock(),
            keyMapRepository = mock(),
            preferenceRepository = mock()
        )
    }

    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }

    /**
     * issue #593
     */
    @Test
    fun `key map with hold down action, load key map, hold down flag shouldn't disappear`() =
        coroutineScope.runBlockingTest {
            //given
            val action = KeyMapAction(
                data = TapCoordinateAction(100, 100, null),
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
        coroutineScope.runBlockingTest {
            KeyEventUtils.MODIFIER_KEYCODES.forEach { keyCode ->
                useCase.mapping.value = State.Data(KeyMap())

                useCase.addAction(KeyEventAction(keyCode))

                useCase.mapping.value.dataOrNull()!!.actionList
                    .single()
                    .let {
                        assertThat(it.holdDown, `is`(true))
                        assertThat(it.repeat, `is`(false))
                    }
            }
        }
}