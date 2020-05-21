package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.data.*
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.delegate.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by sds100 on 21/05/20.
 */
@ExperimentalCoroutinesApi
class ConfigKeymapViewModelTest : IOnboardingState {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var mRepository: KeymapRepository
    private lateinit var mDeviceInfoRepository: DeviceInfoRepository
    private lateinit var mViewModel: ConfigKeymapViewModel

    @Before
    fun init() {
        Dispatchers.setMain(Dispatchers.Default)

        mRepository = FakeKeymapRepository()
        mDeviceInfoRepository = FakeDeviceInfoRepository()
    }

    @Test
    fun dualKeyTrigger_keyRemoved_isSequence() {
        //GIVEN
        val trigger = Trigger(keys = listOf(
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN),
            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP)
        )).apply {
            mode = Trigger.PARALLEL
        }

        val keymap = KeyMap(0, trigger)

        runBlocking {
            mRepository.createKeymap(keymap)
        }

        createViewModel(0)

        //WHEN
        mViewModel.removeTriggerKey(KeyEvent.KEYCODE_VOLUME_UP)

        //THEN
        val value = mViewModel.triggerInSequence.getOrAwaitValue()

        assert(value)
    }

    @Test
    fun noTrigger_keyAdded_isSequence() {
        //GIVEN
        val trigger = Trigger(keys = listOf())

        val keymap = KeyMap(0, trigger)

        runBlocking {
            mRepository.createKeymap(keymap)
        }

        createViewModel(0)

        //WHEN
        runBlocking {
            mViewModel.addTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                "internal_device",
                "internal_device_name",
                isExternal = false)
        }

        //THEN
        val value = mViewModel.triggerInSequence.getOrAwaitValue()

        assert(value)
    }

    fun createViewModel(id: Long) {
        mViewModel = ConfigKeymapViewModel(
            mRepository,
            mDeviceInfoRepository,
            this,
            id
        )
    }

    override fun getShownPrompt(key: Int) = false
    override fun setShownPrompt(key: Int) {}
}