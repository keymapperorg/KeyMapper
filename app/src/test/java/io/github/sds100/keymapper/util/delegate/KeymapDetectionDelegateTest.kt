package io.github.sds100.keymapper.util.delegate

import android.os.SystemClock
import android.view.KeyEvent
import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.SystemAction
import io.github.sds100.keymapper.util.SystemActionUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Created by sds100 on 17/05/2020.
 */

class KeymapDetectionDelegateTest : LifecycleOwner {

    companion object {
        private const val FAKE_KEYBOARD_DESCRIPTOR = "fake_keyboard"
    }

    private val mIKeymapDetectionDelegate = object : IKeymapDetectionDelegate {
        override fun performAction(action: Action) {}

        override fun imitateButtonPress(keyCode: Int) {}

        override val lifecycleScope: LifecycleCoroutineScope
            get() = mLifecycleRegistry.coroutineScope

    }

    private lateinit var mLifecycleRegistry: LifecycleRegistry
    private lateinit var mKeymapDetectionDelegate: KeymapDetectionDelegate

    @Before
    fun createDelegate() {
        mLifecycleRegistry = LifecycleRegistry(this)
        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        mKeymapDetectionDelegate = KeymapDetectionDelegate(mIKeymapDetectionDelegate)
    }

    @Test
    fun onKeyEvent_shortPressParallelTrigger_actionPerformed() {
        //GIVEN
        val trigger = Trigger(
            keys = listOf(
                Trigger.Key(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    Trigger.Key.DEVICE_ID_THIS_DEVICE
                )
//                Trigger.Key(
//                    KeyEvent.KEYCODE_VOLUME_UP,
//                    Trigger.Key.DEVICE_ID_THIS_DEVICE
//                )
            )
        )

        val action = Action(ActionType.SYSTEM_ACTION, data = SystemAction.TOGGLE_FLASHLIGHT)

        val keymap = KeyMap(0, trigger)

        val delegate = mock(KeymapDetectionDelegate::class.java)

        delegate.keyMapListCache = listOf(keymap)

        //WHEN
        delegate.onKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, SystemClock.uptimeMillis(), "", isExternal = false)
        delegate.onKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, SystemClock.uptimeMillis(), "", isExternal = false)

        //THEN
        verify(delegate).performAction(action)
    }

    @After
    fun endLifecycle() {
        mLifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle() = mLifecycleRegistry
}