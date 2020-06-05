package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.data.*
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.delegate.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule

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

    fun createViewModel(id: Long = ConfigKeymapViewModel.NEW_KEYMAP_ID) {
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