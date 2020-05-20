package io.github.sds100.keymapper.util.delegate

import android.os.SystemClock
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.SystemAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * Created by sds100 on 17/05/2020.
 */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class KeymapDetectionDelegateTest {

    companion object {
        private const val FAKE_KEYBOARD_DESCRIPTOR = "fake_keyboard"
        private val FAKE_DESCRIPTORS = arrayOf(
            FAKE_KEYBOARD_DESCRIPTOR
        )

        private val TEST_ACTION = Action(ActionType.SYSTEM_ACTION, SystemAction.TOGGLE_FLASHLIGHT)
    }

    private lateinit var mDelegate: KeymapDetectionDelegate
    private val mTestScope = TestCoroutineScope()

    @Before
    fun init() {
        mDelegate = KeymapDetectionDelegate(mTestScope)
    }

    @Test
    fun shortPressTrigger_validSequenceInput_actionPerformed() = validSequenceInput_actionPerformed(
        Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE)
    )

    @Test
    fun shortPressTrigger_validSequenceInput_downConsumed() = validSequenceInput_downConsumed(
        Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN, Trigger.Key.DEVICE_ID_THIS_DEVICE)
    )

    private fun validSequenceInput_downConsumed(key: Trigger.Key) {
        mDelegate.keyMapListCache = listOf(KeyMap(
            0,
            Trigger(listOf(key)).apply { mode = Trigger.SEQUENCE },
            listOf(TEST_ACTION)
        ))

        //WHEN
        val consumed = inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceIdToDescriptor(key.deviceId))

        //THEN
        assertEquals(consumed, true)
    }

    private fun validSequenceInput_actionPerformed(vararg key: Trigger.Key) {
        mDelegate.keyMapListCache = listOf(KeyMap(0, Trigger(key.toList()), listOf(TEST_ACTION)))

        //WHEN
        runBlockingTest {
            key.forEach {
                inputTriggerKey(it)
            }
        }

        //THEN
        val value = mDelegate.performAction.getOrAwaitValue()

        assertThat(value.getContentIfNotHandled(), `is`(TEST_ACTION))
    }

    private suspend fun inputTriggerKey(key: Trigger.Key) {
        val deviceDescriptor = deviceIdToDescriptor(key.deviceId)

        inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)

        when (key.clickType) {
            Trigger.SHORT_PRESS -> {
                delay(50)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }

            Trigger.LONG_PRESS -> {
                delay(500)
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
            SystemClock.uptimeMillis(),
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
}