package io.github.sds100.keymapper.system.keyevents

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventUseCase
import io.github.sds100.keymapper.actions.keyevent.ConfigKeyEventViewModel
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
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

/**
 * Created by sds100 on 28/04/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ConfigKeyEventViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)
    private lateinit var viewModel: ConfigKeyEventViewModel
    private lateinit var mockUseCase: ConfigKeyEventUseCase

    private lateinit var inputDevices: MutableStateFlow<List<InputDeviceInfo>>

    @Before
    fun init() {
        Dispatchers.setMain(testDispatcher)
        inputDevices = MutableStateFlow(emptyList<InputDeviceInfo>())

        mockUseCase = mock {
            on { showDeviceDescriptors }.then { MutableStateFlow(false) }
            on { inputDevices }.then { inputDevices }
        }
        viewModel = ConfigKeyEventViewModel(
            useCase = mockUseCase,
            resourceProvider = mock {
                on { getString(any()) }.then { "" }
            }
        )
    }

    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
        Dispatchers.resetMain()
    }

    @Test
    fun `multiple input devices with same descriptor but a different name, choose a device, ensure device with correct name is chosen`() =
        coroutineScope.runBlockingTest {
            //GIVEN
            val fakeDevice1 = InputDeviceInfo(
                descriptor = "bla",
                name = "fake device 1",
                id = 0,
                isExternal = false
            )

            val fakeDevice2 = InputDeviceInfo(
                descriptor = "bla",
                name = "fake device 2",
                id = 1,
                isExternal = false
            )

            //WHEN
            inputDevices.value = listOf(fakeDevice1, fakeDevice2)

            //THEN
            viewModel.chooseDevice(0)
            coroutineScope.advanceUntilIdle()

            assertThat(viewModel.uiState.value.chosenDeviceName, `is`(fakeDevice1.name))

            viewModel.chooseDevice(1)

            assertThat(viewModel.uiState.value.chosenDeviceName, `is`(fakeDevice2.name))
        }
}