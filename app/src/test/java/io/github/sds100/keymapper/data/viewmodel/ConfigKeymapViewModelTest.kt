package io.github.sds100.keymapper.data.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.data.FakeDeviceInfoRepository
import io.github.sds100.keymapper.data.FakeKeymapRepository
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.KeymapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
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