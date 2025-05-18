package io.github.sds100.keymapper.actions.keyevents

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventActionViewModel
import io.github.sds100.keymapper.base.actions.keyevent.ConfigKeyEventUseCase
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ConfigKeyServiceEventActionViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: ConfigKeyEventActionViewModel
    private lateinit var mockUseCase: ConfigKeyEventUseCase

    private lateinit var inputDevices: MutableStateFlow<List<InputDeviceInfo>>

    @Before
    fun init() {
        Dispatchers.setMain(testDispatcher)
        inputDevices = MutableStateFlow(emptyList())

        mockUseCase = mock {
            on { showDeviceDescriptors }.then { MutableStateFlow(false) }
            on { inputDevices }.then { inputDevices }
        }
        viewModel = ConfigKeyEventActionViewModel(
            useCase = mockUseCase,
            resourceProvider = mock {
                on { getString(any()) }.then { "" }
            },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `multiple input devices with same descriptor but a different name, choose a device, ensure device with correct name is chosen`() =
        runTest(testDispatcher) {
            // GIVEN
            val fakeDevice1 = InputDeviceInfo(
                descriptor = "bla",
                name = "fake device 1",
                id = 0,
                isExternal = false,
                isGameController = false,
            )

            val fakeDevice2 = InputDeviceInfo(
                descriptor = "bla",
                name = "fake device 2",
                id = 1,
                isExternal = false,
                isGameController = false,
            )

            // WHEN
            inputDevices.value = listOf(fakeDevice1, fakeDevice2)

            // THEN
            viewModel.chooseDevice(0)
            testScope.advanceUntilIdle()

            assertThat(viewModel.uiState.value.chosenDeviceName, `is`(fakeDevice1.name))

            viewModel.chooseDevice(1)

            assertThat(viewModel.uiState.value.chosenDeviceName, `is`(fakeDevice2.name))
        }
}
