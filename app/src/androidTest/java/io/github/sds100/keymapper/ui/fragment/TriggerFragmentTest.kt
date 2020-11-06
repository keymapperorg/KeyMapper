package io.github.sds100.keymapper.ui.fragment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.FakeDeviceInfoRepository
import io.github.sds100.keymapper.data.FakeKeymapRepository
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.KeymapRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import splitties.experimental.ExperimentalSplittiesApi

/**
 * Created by sds100 on 22/05/20.
 */

@ExperimentalCoroutinesApi
@ExperimentalSplittiesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class TriggerFragmentTest : IOnboardingState {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var mRepository: KeymapRepository
    private lateinit var mDeviceInfoRepository: DeviceInfoRepository

    @Before
    fun init() {

        mRepository = FakeKeymapRepository()
        mDeviceInfoRepository = FakeDeviceInfoRepository()

        ServiceLocator.keymapRepository = mRepository
        ServiceLocator.deviceInfoRepository = mDeviceInfoRepository
    }

//    @Test
//    fun dualTrigger_removeTriggerKey_noCrash() {
//
//        //given
//        val id = 0L
//        val action = Action(ActionType.SYSTEM_ACTION, SystemAction.TOGGLE_FLASHLIGHT)
//
//        val trigger = sequenceTrigger(
//            Trigger.Key(KeyEvent.KEYCODE_VOLUME_DOWN),
//            Trigger.Key(KeyEvent.KEYCODE_VOLUME_UP)
//        )
//
//        val keymap = KeyMap(id, trigger, actionList = listOf(action))
//
//        runBlockingTest {
//            mRepository.insertKeymap(keymap)
//        }
//
//        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
//        navController.setViewModelStore(ViewModelStore())
//        navController.setGraph(R.navigation.nav_config_keymap)
//
//        val scenario = launchFragmentInContainer(themeResId = R.style.AppTheme_NoActionBar) {
//            TriggerFragment(id).also { fragment ->
//                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
//                    if (viewLifecycleOwner != null) {
//                        // The fragment’s view has just been created
//                        Navigation.setViewNavController(fragment.requireView(), navController)
//                    }
//                }
//            }
//        }
//
//        Thread.sleep(5000)
//
//        scenario.onFragment {
//            val viewModel by it.navGraphViewModels<ConfigKeymapViewModel>(R.id.nav_config_keymap)
//            viewModel.removeTriggerKey(KeyEvent.KEYCODE_VOLUME_UP)
//        }
//    }

    override fun getShownPrompt(key: Int) = false
    override fun setShownPrompt(key: Int) {}
}