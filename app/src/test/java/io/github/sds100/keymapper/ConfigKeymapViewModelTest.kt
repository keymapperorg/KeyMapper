package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.data.db.IDataStoreManager
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.ConfigKeymapUseCase
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.util.KeyEventUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 30/01/21.
 */

@ExperimentalCoroutinesApi
class ConfigKeymapViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var mockDeviceInfoRepository: DeviceInfoRepository
    private lateinit var mockKeymapRepository: ConfigKeymapUseCase
    private lateinit var mockDataStoreManager: IDataStoreManager

    private lateinit var viewModel: ConfigKeymapViewModel

    @Before
    fun init() {
        mockKeymapRepository = Mockito.mock(ConfigKeymapUseCase::class.java)
        mockDeviceInfoRepository = Mockito.mock(DeviceInfoRepository::class.java)
        mockDataStoreManager = Mockito.mock(IDataStoreManager::class.java)

        viewModel = ConfigKeymapViewModel(
            mockKeymapRepository,
            mockDeviceInfoRepository,
            mockDataStoreManager
        )

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `add modifier key event action, enable hold down option and disable repeat option`() = coroutineScope.runBlockingTest {
        KeyEventUtils.MODIFIER_KEYCODES.forEach { keyCode ->
            val action = Action.keyCodeAction(keyCode)
            viewModel.actionListViewModel.addAction(action)

            viewModel.actionListViewModel.actionList.value!!
                .single { it.uid == action.uid }
                .let {
                    assertThat("action doesn't have hold down flag", it.flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN))
                    assertThat("action has repeat flag", !it.flags.hasFlag(Action.ACTION_FLAG_REPEAT))
                }
        }
    }
}