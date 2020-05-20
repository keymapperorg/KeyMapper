package io.github.sds100.keymapper.util.delegate

import android.os.SystemClock
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.SystemAction
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.robolectric.annotation.Config

/**
 * Created by sds100 on 17/05/2020.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class KeymapDetectionDelegateTest {

    companion object {
        private const val FAKE_KEYBOARD_DESCRIPTOR = "fake_keyboard"

        private val TEST_ACTION = Action(ActionType.SYSTEM_ACTION, SystemAction.TOGGLE_FLASHLIGHT)
    }

    private lateinit var mDelegate: KeymapDetectionDelegate
    private val mTestScope = TestCoroutineScope()

    @Before
    fun createDelegate() {
        mDelegate = KeymapDetectionDelegate(mTestScope)
    }

    private val mSingleShortPressKeymap = KeyMap(0).apply {
        actionList = listOf(TEST_ACTION)
        trigger = Trigger(
            keys = listOf(
                Trigger.Key(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    Trigger.Key.DEVICE_ID_THIS_DEVICE
                )
            )
        )
    }

    @Test
    fun test

    @Test
    fun shortPressSingleKeyTrigger_actionPerformed() {
        //GIVEN

        mDelegate.keyMapListCache = listOf(mSingleShortPressKeymap)

        //WHEN
        mDelegate.onKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, SystemClock.uptimeMillis(), "", isExternal = false)
        mDelegate.onKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, SystemClock.uptimeMillis(), "", isExternal = false)

        //THEN
        val value = mDelegate.performAction.getOrAwaitValue()

        assertThat(value.getContentIfNotHandled(), `is`(TEST_ACTION))
    }

    @Test
    fun shortPressSingleKeyTrigger_downConsumed() {
        //GIVEN
        mDelegate.keyMapListCache = listOf(mSingleShortPressKeymap)

        //WHEN
        val consume = mDelegate.onKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, SystemClock.uptimeMillis(), "", isExternal = false)

        //THEN
        assert(consume)
    }

    @Parameters
    private fun validKeyInput_actionPerformed(trigger: Trigger): Boolean {
    }

    private suspend fun sendDownUp(keycode: Int) = runBlockingTest {
        mDelegate.onKeyEvent(keycode, KeyEvent.ACTION_DOWN, SystemClock.uptimeMillis(), "", isExternal = false)
        delay(100)
        mDelegate.onKeyEvent(keycode, KeyEvent.ACTION_UP, SystemClock.uptimeMillis(), "", isExternal = false)
    }
}