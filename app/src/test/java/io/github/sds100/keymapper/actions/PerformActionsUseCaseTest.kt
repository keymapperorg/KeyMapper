package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeMessenger
import io.github.sds100.keymapper.system.keyevents.InputKeyModel
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Created by sds100 on 01/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformActionsUseCaseTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var useCase: PerformActionsUseCaseImpl
    private lateinit var mockKeyMapperImeMessenger: KeyMapperImeMessenger

    @Before
    fun init() {
        mockKeyMapperImeMessenger = mock()
        useCase = PerformActionsUseCaseImpl(
            coroutineScope,
            accessibilityService = mock(),
            inputMethodAdapter = mock(),
            fileAdapter = mock(),
            suAdapter = mock(),
            shellAdapter = mock(),
            intentAdapter = mock(),
            getActionError = mock(),
            keyMapperImeMessenger = mockKeyMapperImeMessenger,
            packageManagerAdapter = mock(),
            appShortcutAdapter = mock(),
            popupMessageAdapter = mock(),
            deviceAdapter = FakeDevicesAdapter(),
            phoneAdapter = mock(),
            volumeAdapter = mock(),
            cameraAdapter = mock(),
            displayAdapter = mock(),
            lockScreenAdapter = mock(),
            mediaAdapter = mock(),
            airplaneModeAdapter = mock(),
            networkAdapter = mock(),
            bluetoothAdapter = mock(),
            nfcAdapter = mock(),
            openUrlAdapter = mock(),
            resourceProvider = mock()
        )
    }

    @Test
    fun `perform key event action with no device name, ensure action is still performed`() =
        coroutineScope.runBlockingTest {

            val descriptor = "fake_device_descriptor"

            val action = KeyEventAction(
                keyCode = 1,
                metaState = 0,
                useShell = false,
                device = KeyEventAction.Device(descriptor = descriptor, name = "")
            )

            useCase.perform(action, inputEventType = InputEventType.DOWN_UP, keyMetaState = 0)

            verify(mockKeyMapperImeMessenger, times(1)).inputKeyEvent(
                InputKeyModel(
                    keyCode = 1,
                    inputType = InputEventType.DOWN_UP,
                    metaState = 0,
                    deviceId = -1,
                    scanCode = 0,
                    repeat = 0
                )
            )
        }
}