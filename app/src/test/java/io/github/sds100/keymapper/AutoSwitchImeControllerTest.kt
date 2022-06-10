package io.github.sds100.keymapper

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputmethod.AutoSwitchImeController
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

/**
 * Created by sds100 on 25/04/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AutoSwitchImeControllerTest {

    companion object {
        private const val KEY_MAPPER_IME_ID = "key_mapper_keyboard_id"
        private const val NORMAL_IME_ID = "proper_keyboard_id"

        private val FAKE_KEYBOARD = InputDeviceInfo(
            descriptor = "fake_keyboard_descriptor",
            name = "fake keyboard",
            id = 1,
            isExternal = true,
            isGameController = false
        )

        private val FAKE_CONTROLLER = InputDeviceInfo(
            descriptor = "fake_controller_descriptor",
            name = "fake controller",
            id = 2,
            isExternal = true,
            isGameController = true
        )

        private val KEY_MAPPER_IME = ImeInfo(
            id = KEY_MAPPER_IME_ID,
            packageName = Constants.PACKAGE_NAME,
            label = "label",
            isEnabled = true,
            isChosen = false
        )

        private val NORMAL_IME = ImeInfo(
            id = NORMAL_IME_ID,
            packageName = "other.example.app",
            label = "normal keyboard",
            isEnabled = true,
            isChosen = true
        )
    }

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = TestScope(testDispatcher)

    private lateinit var controller: AutoSwitchImeController
    private lateinit var fakePreferenceRepository: FakePreferenceRepository
    private lateinit var mockInputMethodAdapter: InputMethodAdapter
    private lateinit var mockPauseMappingsUseCase: PauseMappingsUseCase
    private lateinit var fakeDevicesAdapter: FakeDevicesAdapter
    private lateinit var mockPopupMessageAdapter: PopupMessageAdapter
    private lateinit var mockResourceProvider: ResourceProvider

    @Before
    fun init() {
        fakePreferenceRepository = FakePreferenceRepository()

        mockInputMethodAdapter = mock {
            on { getInfoByPackageName(Constants.PACKAGE_NAME) }.then {
                Success(KEY_MAPPER_IME)
            }

            on { inputMethodHistory }.then {
                MutableStateFlow(
                    listOf(NORMAL_IME)
                )
            }

            onBlocking { chooseImeWithoutUserInput(KEY_MAPPER_IME_ID) }.then {
                Success(
                    KEY_MAPPER_IME
                )
            }
            onBlocking { chooseImeWithoutUserInput(NORMAL_IME_ID) }.then {
                Success(
                    NORMAL_IME
                )
            }
        }

        fakeDevicesAdapter = FakeDevicesAdapter()

        mockPopupMessageAdapter = mock()

        mockPauseMappingsUseCase = mock {
            on { isPaused }.then { flow<Boolean> { } }
        }

        mockResourceProvider = mock()

        controller = AutoSwitchImeController(
            coroutineScope,
            fakePreferenceRepository,
            mockInputMethodAdapter,
            mockPauseMappingsUseCase,
            fakeDevicesAdapter,
            mockPopupMessageAdapter,
            mockResourceProvider,
            accessibilityServiceAdapter = mock {
                on { eventReceiver }.then { MutableSharedFlow<Event>() }
            }
        )
    }

    @Test
    fun `choose single device, when device connected, show ime picker`() = runTest(testDispatcher) {
        //GIVEN
        val chosenDevices = setOf(FAKE_KEYBOARD.descriptor)

        fakePreferenceRepository.set(Keys.showImePickerOnDeviceConnect, true)
        fakePreferenceRepository.set(Keys.devicesThatShowImePicker, chosenDevices)
        advanceUntilIdle()

        //WHEN
        fakeDevicesAdapter.onInputDeviceConnect.emit(FAKE_KEYBOARD)

        //THEN
        verify(mockInputMethodAdapter, times(1)).showImePicker(fromForeground = false)
    }

    @Test
    fun `choose single device, when device disconnected, show ime picker`() = runTest(testDispatcher) {
        //GIVEN
        val chosenDevices = setOf(FAKE_KEYBOARD.descriptor)

        fakePreferenceRepository.set(Keys.showImePickerOnDeviceConnect, true)
        fakePreferenceRepository.set(Keys.devicesThatShowImePicker, chosenDevices)
        advanceUntilIdle()

        //WHEN
        fakeDevicesAdapter.onInputDeviceDisconnect.emit(FAKE_KEYBOARD)

        //THEN
        verify(mockInputMethodAdapter, times(1)).showImePicker(fromForeground = false)
        }

    @Test
    fun `choose single device, on device disconnect, choose normal keyboard`() = runTest(testDispatcher) {
        //GIVEN
        val chosenDevices = setOf(FAKE_KEYBOARD.descriptor)
        fakePreferenceRepository.set(Keys.devicesThatChangeIme, chosenDevices)
        fakePreferenceRepository.set(Keys.changeImeOnDeviceConnect, true)
        fakePreferenceRepository.set(Keys.showToastWhenAutoChangingIme, true)
        advanceUntilIdle()

        whenever(mockInputMethodAdapter.chosenIme).then { MutableStateFlow(KEY_MAPPER_IME) }

        //WHEN
        fakeDevicesAdapter.onInputDeviceDisconnect.emit(FAKE_KEYBOARD)

        //THEN
        verify(mockInputMethodAdapter, times(1)).chooseImeWithoutUserInput(
            NORMAL_IME_ID,
        )

        verify(mockResourceProvider, times(1)).getString(
            R.string.toast_chose_keyboard,
            NORMAL_IME.label
        )
        verify(mockPopupMessageAdapter, times(1)).showPopupMessage(anyOrNull())
        }

    @Test
    fun `choose single device, when device connected, choose key mapper keyboard`() = runTest(testDispatcher) {
        //GIVEN
        val chosenDevices = setOf(FAKE_KEYBOARD.descriptor)
        fakePreferenceRepository.set(Keys.devicesThatChangeIme, chosenDevices)
        fakePreferenceRepository.set(Keys.changeImeOnDeviceConnect, true)
        fakePreferenceRepository.set(Keys.showToastWhenAutoChangingIme, true)
        advanceUntilIdle()

        whenever(mockInputMethodAdapter.chosenIme).then { MutableStateFlow(NORMAL_IME) }

        //WHEN
        fakeDevicesAdapter.onInputDeviceConnect.emit(FAKE_KEYBOARD)

        //THEN
        verify(mockInputMethodAdapter, times(1)).chooseImeWithoutUserInput(
            KEY_MAPPER_IME_ID,
        )

        verify(mockResourceProvider, times(1)).getString(
            R.string.toast_chose_keyboard,
            KEY_MAPPER_IME.label
        )
        verify(mockPopupMessageAdapter, times(1)).showPopupMessage(anyOrNull())
        }
}