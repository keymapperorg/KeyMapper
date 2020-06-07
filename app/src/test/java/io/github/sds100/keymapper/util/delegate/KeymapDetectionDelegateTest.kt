package io.github.sds100.keymapper.util.delegate

import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.model.Trigger.Companion.DOUBLE_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.LONG_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.SHORT_PRESS
import io.github.sds100.keymapper.util.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Created by sds100 on 17/05/2020.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class KeymapDetectionDelegateTest {

    companion object {
        private const val FAKE_KEYBOARD_DESCRIPTOR = "fake_keyboard"
        private const val FAKE_HEADPHONE_DESCRIPTOR = "fake_headphone"

        private const val FAKE_PACKAGE_NAME = "test_package"

        private val FAKE_DESCRIPTORS = arrayOf(
            FAKE_KEYBOARD_DESCRIPTOR,
            FAKE_HEADPHONE_DESCRIPTOR
        )

        private const val LONG_PRESS_DELAY = 500
        private const val DOUBLE_PRESS_DELAY = 300
        private const val FORCE_VIBRATE = false
        private const val REPEAT_DELAY = 50
        private const val HOLD_DOWN_DELAY = 400
        private const val SEQUENCE_TRIGGER_TIMEOUT = 2000
        private const val VIBRATION_DURATION = 100

        private val TEST_ACTION = Action(ActionType.SYSTEM_ACTION, SystemAction.TOGGLE_FLASHLIGHT)
        private val TEST_ACTION_2 = Action(ActionType.APP, Constants.PACKAGE_NAME)

        private val TEST_ACTIONS = arrayOf(
            TEST_ACTION,
            TEST_ACTION_2
        )
    }

    private lateinit var mDelegate: KeymapDetectionDelegate

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val iClock = object : IClock {
            override val currentTime: Long
                get() = System.currentTimeMillis()
        }

        val iConstraintState = object : IConstraintState {
            override val currentPackageName: String
                get() = FAKE_PACKAGE_NAME
        }

        val preferences = KeymapDetectionPreferences(
            LONG_PRESS_DELAY,
            DOUBLE_PRESS_DELAY,
            HOLD_DOWN_DELAY,
            REPEAT_DELAY,
            SEQUENCE_TRIGGER_TIMEOUT,
            VIBRATION_DURATION,
            FORCE_VIBRATE)

        mDelegate = KeymapDetectionDelegate(GlobalScope, preferences, iClock, iConstraintState)
    }

    @Test
    @Parameters(method = "params_repeatAction")
    fun parallelTrigger_holdDown_repeatAction10Times(description: String, trigger: Trigger) {
        val action = Action(type = ActionType.SYSTEM_ACTION, data = SystemAction.VOLUME_UP)
        val keymap = KeyMap(0, trigger, actionList = listOf(action))
        mDelegate.keyMapListCache = listOf(keymap)

        trigger.keys.forEach {
            inputKeyEvent(it.keyCode, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(it.deviceId))
        }

        val latch = CountDownLatch(10)

        val observer = EventObserver<PerformActionModel> {
            latch.countDown()
        }

        mDelegate.performAction.observeForever(observer)

        assertThat("Failed to repeat 10 times in 2 seconds", latch.await(2, TimeUnit.SECONDS))

        mDelegate.performAction.removeObserver(observer)
        trigger.keys.forEach {
            inputKeyEvent(it.keyCode, KeyEvent.ACTION_UP, deviceIdToDescriptor(it.deviceId))
        }
    }

    fun params_repeatAction() = listOf(
        arrayOf("long press multiple keys", parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, clickType = LONG_PRESS)
        )),
        arrayOf("long press single key", parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS)
        )),
        arrayOf("short press multiple keys", parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = SHORT_PRESS),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, clickType = SHORT_PRESS)
        )),
        arrayOf("short press single key", parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, clickType = SHORT_PRESS)
        ))
    )

    @Test
    @Parameters(method = "params_dualParallelTrigger_input2ndKey_dontConsumeUp")
    fun dualParallelTrigger_input2ndKey_dontConsumeUp(description: String, trigger: Trigger) {
        //given
        val keymap = KeyMap(0, trigger, actionList = listOf(TEST_ACTION))
        mDelegate.keyMapListCache = listOf(keymap)

        runBlocking {
            //when
            trigger.keys[1].let {
                inputKeyEvent(it.keyCode, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(it.deviceId))
            }

            trigger.keys[1].let {
                val consumed = inputKeyEvent(it.keyCode, KeyEvent.ACTION_UP, deviceIdToDescriptor(it.deviceId))

                //then
                assertEquals(false, consumed)
            }
        }
    }

    fun params_dualParallelTrigger_input2ndKey_dontConsumeUp() = listOf(
        arrayOf("long press", parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, clickType = LONG_PRESS)
        )),

        arrayOf("short press", parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = SHORT_PRESS),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, clickType = SHORT_PRESS)
        ))
    )

    @Test
    fun dualShortPressParallelTrigger_validInput_consumeUp() {
        //given
        val trigger = parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP)
        )

        val keymap = KeyMap(0, trigger, actionList = listOf(TEST_ACTION))
        mDelegate.keyMapListCache = listOf(keymap)

        runBlocking {
            //when
            trigger.keys.forEach {
                inputKeyEvent(it.keyCode, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(it.deviceId))
            }

            var consumedUpCount = 0

            trigger.keys.forEach {
                val consumed = inputKeyEvent(it.keyCode, KeyEvent.ACTION_UP, deviceIdToDescriptor(it.deviceId))

                if (consumed) {
                    consumedUpCount += 1
                }
            }

            //then
            assertEquals(2, consumedUpCount)
        }
    }

    @Test
    fun dualLongPressParallelTrigger_validInput_consumeUp() {
        //given
        val trigger = parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, clickType = LONG_PRESS)
        )

        val keymap = KeyMap(0, trigger, actionList = listOf(TEST_ACTION))
        mDelegate.keyMapListCache = listOf(keymap)

        runBlocking {
            //when
            trigger.keys.forEach {
                inputKeyEvent(it.keyCode, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(it.deviceId))
            }

            delay(600)

            var consumedUpCount = 0

            trigger.keys.forEach {
                val consumed = inputKeyEvent(it.keyCode, KeyEvent.ACTION_UP, deviceIdToDescriptor(it.deviceId))

                if (consumed) {
                    consumedUpCount += 1
                }
            }

            //then
            assertEquals(2, consumedUpCount)
        }
    }

    @Test
    fun keyMappedToSingleLongPressAndDoublePress_invalidLongPress_imitateOnce() {
        //given
        val longPressKeymap = createValidKeymapFromTriggerKey(0,
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS))

        val doublePressKeymap = createValidKeymapFromTriggerKey(1,
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = DOUBLE_PRESS))

        mDelegate.keyMapListCache = listOf(longPressKeymap, doublePressKeymap)

        //when

        runBlocking {
            mockTriggerKeyInput(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN))
        }

        //then
        assertNull(mDelegate.imitateButtonPress.value?.getContentIfNotHandled())
        Thread.sleep(500)
        assertEquals(KeyEvent.KEYCODE_VOLUME_DOWN, mDelegate.imitateButtonPress.value?.getContentIfNotHandled()?.keyCode)
    }

    @Test
    fun keyMappedToSingleShortPressAndLongPress_validShortPress_onlyPerformActionDontImitateKey() {
        //given
        val shortPressKeymap = createValidKeymapFromTriggerKey(0,
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN))

        val longPressKeymap = createValidKeymapFromTriggerKey(1,
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS))

        mDelegate.keyMapListCache = listOf(shortPressKeymap, longPressKeymap)

        //when

        runBlocking {
            mockTriggerKeyInput(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN))
        }

        //then
        val performEvent = mDelegate.performAction.getOrAwaitValue()

        assertEquals(TEST_ACTION, performEvent.getContentIfNotHandled()?.action)
        assertNull(mDelegate.imitateButtonPress.value?.getContentIfNotHandled())
    }

    @Test
    fun keyMappedToSingleShortPressAndDoublePress_validShortPress_onlyPerformActionDontImitateKey() {
        //given
        val shortPressKeymap = createValidKeymapFromTriggerKey(0,
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN))

        val doublePressKeymap = createValidKeymapFromTriggerKey(1,
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = DOUBLE_PRESS))

        mDelegate.keyMapListCache = listOf(shortPressKeymap, doublePressKeymap)

        //when

        runBlocking {
            mockTriggerKeyInput(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN))
        }

        //then
        val performEvent = mDelegate.performAction.getOrAwaitValue()

        assertEquals(TEST_ACTION, performEvent.getContentIfNotHandled()?.action)

        //wait for the double press to try and imitate the key.
        Thread.sleep(500)
        assertNull(mDelegate.imitateButtonPress.value?.getContentIfNotHandled())
    }

    @Test
    fun singleKeyTriggerAndShortPressParallelTriggerWithSameInitialKey_validSingleKeyTriggerInput_onlyPerformActionDontImitateKey() {
        //given
        val singleKeyKeymap = createValidKeymapFromTriggerKey(0, Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN))
        val parallelTriggerKeymap = createValidKeymapFromTriggerKey(1,
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN), Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP),
            triggerMode = Trigger.PARALLEL)

        mDelegate.keyMapListCache = listOf(singleKeyKeymap, parallelTriggerKeymap)

        //when

        runBlocking {
            mockTriggerKeyInput(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN))
        }

        //then
        val performEvent = mDelegate.performAction.getOrAwaitValue()

        assertNull(mDelegate.imitateButtonPress.value?.getContentIfNotHandled())
        assertEquals(TEST_ACTION, performEvent.getContentIfNotHandled()?.action)
    }

    @Test
    fun longPressSequenceTrigger_invalidLongPress_keyImitated() {
        val trigger = sequenceTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP)
        )

        val keymap = createValidKeymapFromTriggerKey(0, *trigger.keys.toTypedArray())
        mDelegate.keyMapListCache = listOf(keymap)

        runBlocking {
            mockTriggerKeyInput(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = LONG_PRESS), 100)
        }

        val value = mDelegate.imitateButtonPress.getOrAwaitValue()

        assertEquals(KeyEvent.KEYCODE_VOLUME_DOWN, value.getContentIfNotHandled()?.keyCode)
    }

    @Test
    @Parameters(method = "params_multipleActionsPerformed")
    fun validInput_multipleActionsPerformed(description: String, trigger: Trigger) {
        //GIVEN
        val keymap = KeyMap(0, trigger, TEST_ACTIONS.toList())
        mDelegate.keyMapListCache = listOf(keymap)

        //WHEN
        if (keymap.trigger.mode == Trigger.PARALLEL) {
            mockParallelTriggerKeys(*keymap.trigger.keys.toTypedArray())
        } else {
            runBlocking {
                keymap.trigger.keys.forEach {
                    mockTriggerKeyInput(it)
                }
            }
        }

        //THEN
        var actionPerformedCount = 0

        for (i in TEST_ACTIONS.indices) {
            mDelegate.performAction.getOrAwaitValue()
            actionPerformedCount++
        }

        assertEquals(TEST_ACTIONS.size, actionPerformedCount)
    }

    fun params_multipleActionsPerformed() = listOf(
        arrayOf("sequence", sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE))),
        arrayOf("parallel", parallelTrigger(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_THIS_DEVICE)
        ))
    )

    @Test
    @Parameters(method = "params_allTriggerKeyCombinations")
    @TestCaseName("{0}")
    fun invalidInput_downNotConsumed(description: String, keymap: KeyMap) {
        //GIVEN
        mDelegate.keyMapListCache = listOf(keymap)

        //WHEN
        var consumedCount = 0

        keymap.trigger.keys.forEach {
            val consumed = inputKeyEvent(999, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(it.deviceId))

            if (consumed) {
                consumedCount++
            }
        }

        //THEN
        assertEquals(0, consumedCount)
    }

    @Test
    @Parameters(method = "params_allTriggerKeyCombinations")
    @TestCaseName("{0}")
    fun validInput_downConsumed(description: String, keymap: KeyMap) {
        //GIVEN
        mDelegate.keyMapListCache = listOf(keymap)

        var consumedCount = 0

        keymap.trigger.keys.forEach {
            val consumed = inputKeyEvent(it.keyCode, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(it.deviceId))

            if (consumed) {
                consumedCount++
            }
        }

        assertEquals(keymap.trigger.keys.size, consumedCount)
    }

    fun params_allTriggerKeyCombinations(): List<Array<Any>> {
        val triggerAndDescriptions = listOf(
            "sequence single short-press this-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE)),
            "sequence single long-press this-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS)),
            "sequence single double-press this-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = DOUBLE_PRESS)),

            "sequence single short-press any-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_ANY_DEVICE)),
            "sequence single long-press any-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = LONG_PRESS)),
            "sequence single double-press any-device" to sequenceTrigger(Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = DOUBLE_PRESS)),

            "sequence multiple short-press this-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS)
            ),
            "sequence multiple long-press this-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS)
            ),
            "sequence multiple double-press this-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = DOUBLE_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = DOUBLE_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = DOUBLE_PRESS)
            ),
            "sequence multiple mix this-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = DOUBLE_PRESS)
            ),
            "sequence multiple mix external-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_KEYBOARD_DESCRIPTOR, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, FAKE_HEADPHONE_DESCRIPTOR, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_HEADPHONE_DESCRIPTOR, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_DESCRIPTOR, clickType = DOUBLE_PRESS)
            ),

            "sequence multiple short-press mixed-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_KEYBOARD_DESCRIPTOR, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS)
            ),
            "sequence multiple long-press mixed-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_KEYBOARD_DESCRIPTOR, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS)
            ),
            "sequence multiple double-press mixed-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_KEYBOARD_DESCRIPTOR, clickType = DOUBLE_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = DOUBLE_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = DOUBLE_PRESS)
            ),
            "sequence multiple mix mixed-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_KEYBOARD_DESCRIPTOR, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = DOUBLE_PRESS)
            ),
            "sequence multiple mix mixed-device" to sequenceTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, FAKE_HEADPHONE_DESCRIPTOR, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_DESCRIPTOR, clickType = DOUBLE_PRESS)
            ),

            "parallel multiple short-press this-device" to parallelTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS)
            ),
            "parallel multiple long-press this-device" to parallelTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS)
            ),
            "parallel multiple short-press external-device" to parallelTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_KEYBOARD_DESCRIPTOR, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, FAKE_HEADPHONE_DESCRIPTOR, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, FAKE_HEADPHONE_DESCRIPTOR, clickType = SHORT_PRESS)
            ),
            "parallel multiple long-press external-device" to parallelTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, FAKE_HEADPHONE_DESCRIPTOR, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, FAKE_HEADPHONE_DESCRIPTOR, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, FAKE_HEADPHONE_DESCRIPTOR, clickType = LONG_PRESS)
            ),
            "parallel multiple short-press mix-device" to parallelTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = SHORT_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_DESCRIPTOR, clickType = SHORT_PRESS)
            ),
            "parallel multiple long-press mix-device" to parallelTrigger(
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP, Trigger.Key.DEVICE_ID_ANY_DEVICE, clickType = LONG_PRESS),
                Trigger.Key(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_DESCRIPTOR, clickType = LONG_PRESS)
            )
        )

        return triggerAndDescriptions.mapIndexed { i, triggerAndDescription ->
            arrayOf(triggerAndDescription.first, KeyMap(i.toLong(), triggerAndDescription.second, listOf(TEST_ACTION)))
        }
    }

    @Test
    @Parameters(method = "params_allTriggerKeyCombinations")
    @TestCaseName("{0}")
    fun validInput_actionPerformed(description: String, keymap: KeyMap) {
        mDelegate.keyMapListCache = listOf(keymap)

        //WHEN
        if (keymap.trigger.mode == Trigger.PARALLEL) {
            mockParallelTriggerKeys(*keymap.trigger.keys.toTypedArray())
        } else {
            runBlocking {
                keymap.trigger.keys.forEach {
                    mockTriggerKeyInput(it)
                }
            }
        }

        //THEN
        val value = mDelegate.performAction.getOrAwaitValue()

        assertThat(value.getContentIfNotHandled()?.action, `is`(TEST_ACTION))
    }

    private suspend fun mockTriggerKeyInput(key: Trigger.Key, delay: Long? = null) {
        val deviceDescriptor = deviceIdToDescriptor(key.deviceId)
        val pressDelay: Long = delay ?: when (key.clickType) {
            LONG_PRESS -> 600L
            else -> 50L
        }

        inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)

        when (key.clickType) {
            SHORT_PRESS -> {
                delay(pressDelay)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }

            LONG_PRESS -> {
                delay(pressDelay)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }

            DOUBLE_PRESS -> {
                delay(pressDelay)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
                delay(pressDelay)

                inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)
                delay(pressDelay)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }
        }
    }

    private fun inputKeyEvent(keyCode: Int, action: Int, deviceDescriptor: String?) =
        mDelegate.onKeyEvent(
            keyCode,
            action,
            deviceDescriptor ?: "",
            isExternal = deviceDescriptor != null,
            metaState = 0
        )

    private fun mockParallelTriggerKeys(
        vararg key: Trigger.Key,
        delay: Long? = null) {
        key.forEach {
            val deviceDescriptor = deviceIdToDescriptor(it.deviceId)

            inputKeyEvent(it.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)
        }

        GlobalScope.launch {
            if (delay != null) {
                delay(delay)
            } else {
                when (key[0].clickType) {
                    SHORT_PRESS -> delay(50)
                    LONG_PRESS -> delay(600)
                }
            }

            key.forEach {
                val deviceDescriptor = deviceIdToDescriptor(it.deviceId)

                inputKeyEvent(it.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }
        }
    }

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

    private fun createValidKeymapFromTriggerKey(id: Long, vararg key: Trigger.Key, triggerMode: Int = Trigger.SEQUENCE) =
        KeyMap(id, Trigger(keys = key.toList()).apply { mode = triggerMode }, actionList = listOf(TEST_ACTION))
}