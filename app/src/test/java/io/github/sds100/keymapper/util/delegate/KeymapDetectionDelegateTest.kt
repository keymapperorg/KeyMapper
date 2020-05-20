package io.github.sds100.keymapper.util.delegate

import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.IClock
import io.github.sds100.keymapper.util.SystemAction
import junit.framework.Assert.assertEquals
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Created by sds100 on 17/05/2020.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class KeymapDetectionDelegateTest {

    companion object {
        private const val FAKE_KEYBOARD_DESCRIPTOR = "fake_keyboard"
        private val FAKE_DESCRIPTORS = arrayOf(
            FAKE_KEYBOARD_DESCRIPTOR
        )
        private val LONG_PRESS_DELAY = 500
        private val DOUBLE_PRESS_DELAY = 300

        private val TEST_ACTION = Action(ActionType.SYSTEM_ACTION, SystemAction.TOGGLE_FLASHLIGHT)
    }

    private lateinit var mDelegate: KeymapDetectionDelegate
    private val mTestScope = TestCoroutineScope()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val iClock = object : IClock {
            override val currentTime: Long
                get() = System.currentTimeMillis()
        }

        mDelegate = KeymapDetectionDelegate(mTestScope, LONG_PRESS_DELAY, DOUBLE_PRESS_DELAY, iClock)
    }

    @Test
    @Parameters(method = "params_downConsumed")
    @TestCaseName("{0}")
    fun invalidInput_downNotConsumed(description: String, key: Trigger.Key) {
        //GIVEN
        val keymap = createKeymapFromTriggerKey(0, key)
        mDelegate.keyMapListCache = listOf(keymap)

        //WHEN
        val consumed = inputKeyEvent(KeyEvent.KEYCODE_0, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(key.deviceId))

        //THEN
        assertEquals(consumed, false)
    }

    @Test
    @Parameters(method = "params_downConsumed")
    @TestCaseName("{0}")
    fun validInput_downConsumed(description: String, key: Trigger.Key) {
        //GIVEN
        val keymap = createKeymapFromTriggerKey(0, key)
        mDelegate.keyMapListCache = listOf(keymap)

        //WHEN
        val consumed = inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(key.deviceId))

        //THEN
        assertEquals(consumed, true)
    }

    fun params_downConsumed() = listOf(
        arrayOf("short press", Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE)),
        arrayOf("long press", Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = Trigger.LONG_PRESS)),
        arrayOf("double press", Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = Trigger.DOUBLE_PRESS))
    )

    @Test
    @Parameters(method = "params_actionPerformed")
    @TestCaseName("{0}")
    fun validInput_actionPerformed(description: String, keymap: KeyMap) {
        mDelegate.keyMapListCache = listOf(keymap)

        //WHEN
        runBlocking {
            keymap.trigger.keys.forEach {
                mockTriggerKeyInput(it)
            }
        }

        //THEN
        val value = mDelegate.performAction.getOrAwaitValue()

        assertThat(value.getContentIfNotHandled(), `is`(TEST_ACTION))
    }

    fun params_actionPerformed(): List<Array<Any>> {
        val triggerAndDescriptions = listOf(
            "sequence single short-press this-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE)),
            "sequence single long-press this-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = Trigger.LONG_PRESS))
        )

        return triggerAndDescriptions.mapIndexed { i, triggerAndDescription ->
            arrayOf(triggerAndDescription.first, KeyMap(i.toLong(), triggerAndDescription.second, listOf(TEST_ACTION)))
        }
    }

    private fun sequenceTrigger(vararg key: Trigger.Key) = Trigger(key.toList()).apply { mode = Trigger.SEQUENCE }
    private fun parallelTrigger(vararg key: Trigger.Key) = Trigger(key.toList()).apply { mode = Trigger.PARALLEL }

    private suspend fun mockTriggerKeyInput(key: Trigger.Key) {
        val deviceDescriptor = deviceIdToDescriptor(key.deviceId)

        inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)

        when (key.clickType) {
            Trigger.SHORT_PRESS -> {
                delay(50)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }

            Trigger.LONG_PRESS -> {
                delay(600)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }

            Trigger.DOUBLE_PRESS -> {
                delay(50)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
                delay(100)

                inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)
                delay(50)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }
        }
    }

    private fun inputKeyEvent(keyCode: Int, action: Int, deviceDescriptor: String?) =
        mDelegate.onKeyEvent(
            keyCode,
            action,
            deviceDescriptor ?: "",
            isExternal = deviceDescriptor != null
        )

    private fun deviceIdToDescriptor(deviceId: String): String? {
        return when (deviceId) {
            Trigger.Key.DEVICE_ID_THIS_DEVICE -> null
            Trigger.Key.DEVICE_ID_ANY_DEVICE -> {
                val randomInt = Random.nextInt(-1, FAKE_DESCRIPTORS.lastIndex)

                if (randomInt == -1) {
                    ""
                } else {
                    FAKE_DESCRIPTORS[randomInt]
                }
            }
            else -> deviceId
        }
    }

    private fun createKeymapFromTriggerKey(id: Long, key: Trigger.Key) =
        KeyMap(id, Trigger(keys = listOf(key)), actionList = listOf(TEST_ACTION))
}